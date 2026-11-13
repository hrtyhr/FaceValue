package com.course.facevalue.analysis

import android.graphics.PointF
import com.course.facevalue.analysis.model.Box
import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.Pt
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour

/**
 * 把 ML Kit 的 Face（含 13 组 contour）转换为与平台无关的 [FaceLandmarks]。
 *
 * ML Kit 在检测前会按 InputImage 的 rotationDegrees 旋转图像，结果坐标位于
 * “摆正后”的图像坐标系中，因此宽高需要在旋转 90/270 时互换。
 */
object MlKitFaceMapper {

    fun toLandmarks(face: Face, imageWidth: Int, imageHeight: Int, rotationDegrees: Int): FaceLandmarks? {
        val swap = rotationDegrees % 180 != 0
        val upW = if (swap) imageHeight else imageWidth
        val upH = if (swap) imageWidth else imageHeight
        if (upW <= 0 || upH <= 0) return null

        fun pts(type: Int): List<Pt> = face.getContour(type)?.points.orEmpty().map {
            Pt(it.x / upW, it.y / upH)
        }

        val facePts = pts(FaceContour.FACE)
        val leftBrowTop = pts(FaceContour.LEFT_EYEBROW_TOP)
        val leftBrowBottom = pts(FaceContour.LEFT_EYEBROW_BOTTOM)
        val rightBrowTop = pts(FaceContour.RIGHT_EYEBROW_TOP)
        val rightBrowBottom = pts(FaceContour.RIGHT_EYEBROW_BOTTOM)
        val leftEye = pts(FaceContour.LEFT_EYE)
        val rightEye = pts(FaceContour.RIGHT_EYE)
        val noseBridge = pts(FaceContour.NOSE_BRIDGE)
        val noseBottom = pts(FaceContour.NOSE_BOTTOM)
        val upperLipTop = pts(FaceContour.UPPER_LIP_TOP)
        val upperLipBottom = pts(FaceContour.UPPER_LIP_BOTTOM)
        val lowerLipTop = pts(FaceContour.LOWER_LIP_TOP)
        val lowerLipBottom = pts(FaceContour.LOWER_LIP_BOTTOM)
        val leftCheek = pts(FaceContour.LEFT_CHEEK)
        val rightCheek = pts(FaceContour.RIGHT_CHEEK)

        val landmarks = FaceLandmarks(
            face = facePts,
            // 眉毛的上/下轮廓是两段独立弧线；直接拼接会在两段之间产生跨眉线段，
            // 使重采样与绘制出现偏离轮廓的点，因此取两条弧线的中线。
            leftBrow = browMidline(leftBrowTop, leftBrowBottom),
            rightBrow = browMidline(rightBrowTop, rightBrowBottom),
            leftEye = leftEye,
            rightEye = rightEye,
            noseBridge = noseBridge,
            noseBottom = noseBottom,
            upperLipTop = upperLipTop,
            upperLipBottom = upperLipBottom,
            lowerLipTop = lowerLipTop,
            lowerLipBottom = lowerLipBottom,
            leftCheek = leftCheek,
            rightCheek = rightCheek,
            bounds = face.boundingBox.toBox(upW, upH),
            rollDeg = face.headEulerAngleZ,
            yawDeg = face.headEulerAngleY,
            pitchDeg = face.headEulerAngleX,
            smilingProbability = face.smilingProbability,
            leftEyeOpenProbability = face.leftEyeOpenProbability,
            rightEyeOpenProbability = face.rightEyeOpenProbability,
            trackingId = face.trackingId,
            imageWidth = upW,
            imageHeight = upH,
            detectedPointCount = facePts.size + leftBrowTop.size + leftBrowBottom.size +
                rightBrowTop.size + rightBrowBottom.size + leftEye.size + rightEye.size +
                noseBridge.size + noseBottom.size + upperLipTop.size + upperLipBottom.size +
                lowerLipTop.size + lowerLipBottom.size + leftCheek.size + rightCheek.size
        )
        return if (landmarks.hasCoreContours()) landmarks else null
    }

    fun toLandmarks(face: Face, bitmapWidth: Int, bitmapHeight: Int): FaceLandmarks? =
        toLandmarks(face, bitmapWidth, bitmapHeight, 0)

    /** 把眉毛上下两条弧线按最近端点对齐后逐点平均，得到无跳变的中线。 */
    private fun browMidline(top: List<Pt>, bottom: List<Pt>): List<Pt> {
        if (top.isEmpty()) return bottom
        if (bottom.isEmpty()) return top
        val count = maxOf(top.size, bottom.size)
        fun at(points: List<Pt>, index: Int): Pt {
            if (points.size == 1 || count == 1) return points.first()
            val t = index.toFloat() * (points.size - 1) / (count - 1)
            val a = kotlin.math.floor(t).toInt().coerceIn(0, points.size - 1)
            val b = (a + 1).coerceAtMost(points.size - 1)
            val f = t - a
            val pa = points[a]
            val pb = points[b]
            return Pt(pa.x + (pb.x - pa.x) * f, pa.y + (pb.y - pa.y) * f)
        }
        val direct = top.first().distanceTo(bottom.first()) + top.last().distanceTo(bottom.last())
        val reversed = top.first().distanceTo(bottom.last()) + top.last().distanceTo(bottom.first())
        val ordered = if (direct <= reversed) bottom else bottom.reversed()
        return List(count) { i -> (at(top, i) + at(ordered, i)) * 0.5f }
    }

    private fun android.graphics.Rect.toBox(w: Int, h: Int): Box = Box(
        left.coerceIn(0, w) / w.toFloat(),
        top.coerceIn(0, h) / h.toFloat(),
        right.coerceIn(0, w) / w.toFloat(),
        bottom.coerceIn(0, h) / h.toFloat()
    )
}
