package com.course.facevalue.analysis

import android.graphics.Bitmap
import android.os.SystemClock
import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.FaceMeasurements
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/** 单张图片的完整分析流水线：检测 → 关键点 → 几何特征 → 肤色采样 → 评分。 */
class FaceAnalysisEngine(
    private val accurate: Boolean = true
) : AutoCloseable {

    private val detector = FaceDetectorWrapper(accurate = accurate, tracking = false, minFaceSize = 0.10f)
    private val closed = AtomicBoolean(false)

    suspend fun analyze(
        bitmap: Bitmap,
        profile: GenderProfile,
        source: ImageSource
    ): AnalysisOutcome = withContext(Dispatchers.Default) {
        val started = SystemClock.elapsedRealtime()
        check(!closed.get()) { "分析器已关闭" }

        val faces = detector.detect(InputImage.fromBitmap(bitmap, 0))
        if (faces.isEmpty()) throw NoFaceException()
        val face = faces.maxByOrNull {
            it.boundingBox.width().toLong() * it.boundingBox.height().toLong()
        } ?: throw NoFaceException()

        val landmarks = MlKitFaceMapper.toLandmarks(face, bitmap.width, bitmap.height)
            ?: throw LandmarkException()
        val measurements = GeometryCalculator.measure(landmarks) ?: throw LandmarkException()
        val skin = SkinAnalyzer.fromBitmap(bitmap, landmarks)
        val score = ScoringEngine.score(measurements, skin, profile)

        AnalysisOutcome(
            bitmap = bitmap,
            landmarks = landmarks,
            measurements = measurements,
            skin = skin,
            score = score,
            profile = profile,
            source = source,
            elapsedMs = SystemClock.elapsedRealtime() - started,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            detector.close()
        }
    }

    /** 仅用于实验：不经过评分文案，直接返回原始测量。 */
    suspend fun measureOnly(bitmap: Bitmap): Pair<FaceLandmarks, FaceMeasurements> =
        withContext(Dispatchers.Default) {
            val faces = detector.detect(InputImage.fromBitmap(bitmap, 0))
            val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                ?: throw NoFaceException()
            val landmarks = MlKitFaceMapper.toLandmarks(face, bitmap.width, bitmap.height)
                ?: throw LandmarkException()
            val measurements = GeometryCalculator.measure(landmarks) ?: throw LandmarkException()
            landmarks to measurements
        }
}
