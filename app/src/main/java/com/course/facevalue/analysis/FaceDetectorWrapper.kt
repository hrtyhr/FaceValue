package com.course.facevalue.analysis

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable

/**
 * ML Kit 人脸检测器封装（内置模型，离线运行）。
 * 静态图片使用 ACCURATE 模式获得更稳定的轮廓；实时预览使用 FAST 模式降低延迟。
 */
class FaceDetectorWrapper(
    accurate: Boolean = true,
    tracking: Boolean = false,
    minFaceSize: Float = 0.12f
) : Closeable {

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(
                if (accurate) FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
                else FaceDetectorOptions.PERFORMANCE_MODE_FAST
            )
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(minFaceSize)
            .apply { if (tracking) enableTracking() }
            .build()
    )

    suspend fun detect(image: InputImage): List<Face> = detector.process(image).awaitResult()

    /** 返回未包装的 Task，供调用方在 Task 真正结束后释放输入缓冲区。 */
    fun detectTask(image: InputImage): Task<List<Face>> = detector.process(image)

    override fun close() {
        try {
            detector.close()
        } catch (_: Exception) {
        }
    }
}
