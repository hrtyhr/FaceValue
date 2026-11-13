package com.course.facevalue.camera

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.course.facevalue.analysis.FaceDetectorWrapper
import com.course.facevalue.analysis.GenderProfile
import com.course.facevalue.analysis.GeometryCalculator
import com.course.facevalue.analysis.MlKitFaceMapper
import com.course.facevalue.analysis.ScoringEngine
import com.course.facevalue.analysis.awaitResult
import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.FaceMeasurements
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

data class LiveFrame(
    val landmarks: FaceLandmarks?,
    val measurements: FaceMeasurements?,
    val score: com.course.facevalue.analysis.ScoreResult?,
    val faceCount: Int,
    val fps: Float,
    val processMs: Long,
    val imageWidth: Int,
    val imageHeight: Int
)

/**
 * CameraX 实时分析器。
 *
 * 以约 15 fps 节流处理分析帧，ML Kit FAST 模式 + tracking 提供稳定的人脸框与轮廓；
 * 实时评分使用指数滑动平均（EMA，α=0.35）抑制逐帧抖动。
 */
class LiveFaceAnalyzer(
    private val onFrame: (LiveFrame) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    @Volatile var profile: GenderProfile = GenderProfile.NEUTRAL
    @Volatile var active: Boolean = true

    private val detector = FaceDetectorWrapper(accurate = false, tracking = true, minFaceSize = 0.12f)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean(false)
    private var smoothed: Float? = null
    private var lastFrameAt = 0L
    private var fps = 0f
    private var lastFaceAt = 0L

    override fun analyze(image: ImageProxy) {
        if (!active || closed.get()) {
            image.close()
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastFrameAt < 66) {
            image.close()
            return
        }
        val interval = now - lastFrameAt
        if (lastFrameAt > 0 && interval > 0) {
            val instant = 1000f / interval
            fps = if (fps == 0f) instant else fps * 0.8f + instant * 0.2f
        }
        lastFrameAt = now

        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        val rotation = image.imageInfo.rotationDegrees
        val imageWidth = image.width
        val imageHeight = image.height
        val swap = rotation % 180 != 0
        val uprightW = if (swap) imageHeight else imageWidth
        val uprightH = if (swap) imageWidth else imageHeight

        val started = SystemClock.elapsedRealtime()
        val inputImage = try {
            InputImage.fromMediaImage(mediaImage, rotation)
        } catch (e: Exception) {
            image.close()
            postEmptyFrame(started, uprightW, uprightH)
            return
        }
        val task = try {
            detector.detectTask(inputImage)
        } catch (e: Exception) {
            image.close()
            postEmptyFrame(started, uprightW, uprightH)
            return
        }
        // 即使 withTimeout 放弃了 await，ML Kit 的 Task 仍可能读取缓冲区，
        // 因此必须等 Task 真正结束后再关闭 ImageProxy。
        task.addOnCompleteListener { image.close() }

        try {
            val faces = runBlocking { withTimeout(2500) { task.awaitResult() } }
            val face = faces.maxByOrNull {
                it.boundingBox.width().toLong() * it.boundingBox.height().toLong()
            }
            var landmarks: FaceLandmarks? = null
            var measurements: FaceMeasurements? = null
            var score: com.course.facevalue.analysis.ScoreResult? = null
            if (face != null) {
                landmarks = MlKitFaceMapper.toLandmarks(face, imageWidth, imageHeight, rotation)
                measurements = landmarks?.let { GeometryCalculator.measure(it) }
                if (landmarks != null && measurements != null) {
                    val result = ScoringEngine.score(measurements, null, profile)
                    val previous = smoothed
                    val next = if (previous == null) result.raw else previous + 0.35f * (result.raw - previous)
                    smoothed = next
                    score = result.copy(raw = next)
                    lastFaceAt = now
                }
            }
            if (now - lastFaceAt > 1200) smoothed = null
            val frame = LiveFrame(
                landmarks = landmarks,
                measurements = measurements,
                score = score,
                faceCount = faces.size,
                fps = fps,
                processMs = SystemClock.elapsedRealtime() - started,
                imageWidth = uprightW,
                imageHeight = uprightH
            )
            mainHandler.post { if (active && !closed.get()) onFrame(frame) }
        } catch (e: Exception) {
            postEmptyFrame(started, uprightW, uprightH)
        }
    }

    private fun postEmptyFrame(started: Long, uprightW: Int, uprightH: Int) {
        mainHandler.post {
            if (active && !closed.get()) {
                onFrame(
                    LiveFrame(
                        null, null, null, 0, fps,
                        SystemClock.elapsedRealtime() - started, uprightW, uprightH
                    )
                )
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            active = false
            detector.close()
            mainHandler.removeCallbacksAndMessages(null)
        }
    }
}
