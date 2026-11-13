package com.course.facevalue.analysis

import android.os.SystemClock
import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.FaceMeasurements
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ImageSource { GALLERY, CAMERA, SAMPLE, LIVE }

class NoFaceException : Exception("未检测到人脸。请让面部完整出现在画面中，并保持光线充足。")
class LandmarkException : Exception("检测到人脸，但轮廓关键点不足，无法完成几何测量。请换一张正脸、无遮挡的照片。")

data class AnalysisOutcome(
    val bitmap: android.graphics.Bitmap,
    val landmarks: FaceLandmarks,
    val measurements: FaceMeasurements,
    val skin: SkinStats?,
    val score: ScoreResult,
    val profile: GenderProfile,
    val source: ImageSource,
    val elapsedMs: Long,
    val imageWidth: Int,
    val imageHeight: Int,
    val analysisId: String = ""
)

/** ML Kit Task → 协程。 */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
    addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    addOnCanceledListener { if (cont.isActive) cont.cancel() }
}
