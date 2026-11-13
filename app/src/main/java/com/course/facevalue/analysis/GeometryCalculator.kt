package com.course.facevalue.analysis

import com.course.facevalue.analysis.model.Box
import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.FaceMeasurements
import com.course.facevalue.analysis.model.Pt
import com.course.facevalue.analysis.model.angleDegrees
import com.course.facevalue.analysis.model.bounds
import com.course.facevalue.analysis.model.centroid
import com.course.facevalue.analysis.model.rotated
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 纯 Kotlin 的几何特征计算，不依赖 Android 框架，可直接在 JVM 单元测试中验证。
 *
 * 处理流程：
 * 1. 以两眼连线为水平基准做滚转校正（roll correction）；
 * 2. 以 ML Kit 人脸检测框顶边近似“发际线”（检测框包含额部），计算三庭；
 * 3. 五眼比例采用以面部中线为中心的对称参考宽度，避免头发/遮挡造成的轮廓偏移；
 * 4. 对称性只使用稳定的眼睛与嘴角锚点，并按照左右眼宽度做轻微的姿态归一化。
 */
object GeometryCalculator {

    private const val EPS = 1e-4f

    fun measure(lm: FaceLandmarks): FaceMeasurements? {
        if (!lm.hasCoreContours()) return null

        val rawEyeA = lm.leftEye.centroid()
        val rawEyeB = lm.rightEye.centroid()
        val eyeLine = Math.toDegrees(
            atan2((rawEyeB.y - rawEyeA.y).toDouble(), (rawEyeB.x - rawEyeA.x).toDouble())
        ).toFloat()
        val center = rawEyeA.mid(rawEyeB)
        val correction = -eyeLine

        fun rot(points: List<Pt>): List<Pt> = points.rotated(center, correction)

        val face = rot(lm.face)
        val eye1 = rot(lm.leftEye)
        val eye2 = rot(lm.rightEye)
        val brow1 = rot(lm.leftBrow)
        val brow2 = rot(lm.rightBrow)
        val noseBottom = rot(lm.noseBottom)
        val upperTop = rot(lm.upperLipTop)
        val upperBottom = rot(lm.upperLipBottom)
        val lowerTop = rot(lm.lowerLipTop)
        val lowerBottom = rot(lm.lowerLipBottom)

        if (face.size < 8 || brow1.isEmpty() || brow2.isEmpty() || noseBottom.size < 2 ||
            upperTop.size < 3 || upperBottom.size < 3 || lowerTop.size < 3 || lowerBottom.size < 3
        ) return null

        // 两眼按图像 x 排序，后续不依赖 ML Kit 的左右语义。
        val (eyeA, eyeB) = if (eye1.centroid().x <= eye2.centroid().x) eye1 to eye2 else eye2 to eye1
        val (browA, browB) = if (brow1.centroid().x <= brow2.centroid().x) brow1 to brow2 else brow2 to brow1

        val faceBox = face.bounds()
        val chinY = faceBox.bottom
        // 检测框顶边通常位于发际线附近，而 FACE 轮廓点只到额部中段；
        // 两者取更靠上的位置，避免上庭被系统性低估。检测框需先旋到同一坐标系再比较。
        val boxTopLeft = Pt(lm.bounds.left, lm.bounds.top).rotated(center, correction)
        val boxTopRight = Pt(lm.bounds.right, lm.bounds.top).rotated(center, correction)
        val boxTopY = min(boxTopLeft.y, boxTopRight.y)
        val topY = min(boxTopY, faceBox.top).coerceAtLeast(0f)
        val faceHeight = max(chinY - topY, EPS)
        val faceWidth = max(faceBox.width, EPS)

        val eyeCA = eyeA.centroid()
        val eyeCB = eyeB.centroid()
        val eyeCenter = eyeCA.mid(eyeCB)
        val eyeWidthA = horizontalExtent(eyeA)
        val eyeWidthB = horizontalExtent(eyeB)
        val eyeWidth = max((eyeWidthA + eyeWidthB) / 2f, EPS)
        val eyeHeightA = verticalExtent(eyeA)
        val eyeHeightB = verticalExtent(eyeB)
        val eyeHeight = max((eyeHeightA + eyeHeightB) / 2f, EPS)
        val eyeLevelY = eyeCenter.y

        val browY = (browA.centroid().y + browB.centroid().y) / 2f
        val noseBase = noseBottom.centroid()
        val subnasaleY = noseBase.y

        val mouthPoints = upperTop + lowerBottom
        val mouthCenter = mouthPoints.centroid()
        val mouthCornerLeft = mouthPoints.minByOrNull { it.x } ?: mouthCenter
        val mouthCornerRight = mouthPoints.maxByOrNull { it.x } ?: mouthCenter
        val mouthWidth = max(mouthCornerLeft.distanceTo(mouthCornerRight), EPS)
        val mouthTilt = Math.toDegrees(
            atan2(
                (mouthCornerRight.y - mouthCornerLeft.y).toDouble(),
                (mouthCornerRight.x - mouthCornerLeft.x).toDouble()
            )
        ).toFloat()

        // 唇红高度取唇线中部 50% 的点，避免嘴角处的高度失真。
        val upperLipH = max(meanYInMiddle(upperBottom) - meanYInMiddle(upperTop), 0f)
        val lowerLipH = max(meanYInMiddle(lowerBottom) - meanYInMiddle(lowerTop), 0f)
        val lipHeight = upperLipH + lowerLipH

        val upperThird = max(browY - topY, 0.04f * faceHeight)
        val middleThird = max(subnasaleY - browY, 0f)
        val lowerThird = max(chinY - subnasaleY, 0f)
        val thirdsSum = max(upperThird + middleThird + lowerThird, EPS)
        val thirds = listOf(upperThird / thirdsSum, middleThird / thirdsSum, lowerThird / thirdsSum)

        val eyeAInner = eyeA.maxByOrNull { it.x } ?: eyeCA
        val eyeAOuter = eyeA.minByOrNull { it.x } ?: eyeCA
        val eyeBInner = eyeB.minByOrNull { it.x } ?: eyeCB
        val eyeBOuter = eyeB.maxByOrNull { it.x } ?: eyeCB

        val chinPoints = face.filter { it.y >= chinY - 0.02f * faceHeight }
        val chinPoint = if (chinPoints.isEmpty()) {
            Pt(faceBox.centerX, chinY)
        } else {
            Pt(chinPoints.map { it.x }.average().toFloat(), chinPoints.maxOf { it.y })
        }

        val midXCandidates = listOf(eyeCenter.x, noseBase.x, mouthCenter.x, chinPoint.x).sorted()
        val midX = midXCandidates[midXCandidates.size / 2]

        // 五眼参考宽度：取人脸最大宽度（颧弓附近），以中线为中心对称展开，
        // 消除头发/耳部轮廓遮挡造成的左右端点偏移。
        val (eyeFaceLeft, eyeFaceRight) = horizontalSpan(face, eyeLevelY, 0.03f * faceHeight, faceWidth)
        val eyeLevelWidth = max(max(eyeFaceRight - eyeFaceLeft, faceWidth), EPS)
        val fiveLeft = midX - eyeLevelWidth / 2f
        val fiveRight = midX + eyeLevelWidth / 2f
        val fiveEyesRaw = listOf(
            (eyeAOuter.x - fiveLeft).coerceAtLeast(0f),
            eyeWidthA,
            (eyeBInner.x - eyeAInner.x).coerceAtLeast(0f),
            eyeWidthB,
            (fiveRight - eyeBOuter.x).coerceAtLeast(0f)
        )
        val fiveEyes = fiveEyesRaw.map { it / eyeLevelWidth }

        val noseWidth = max(horizontalExtent(noseBottom), EPS)
        val interocular = max(eyeCA.distanceTo(eyeCB), EPS)

        // 对称性：只用稳定的眼、嘴角锚点；左右眼宽度差异先作为姿态尺度因子，
        // 降低头部轻微转动造成的伪不对称。
        val scaleA = eyeWidth / max(eyeWidthA, EPS)
        val scaleB = eyeWidth / max(eyeWidthB, EPS)
        val pairs = listOf(
            eyeAOuter to eyeBOuter,
            eyeAInner to eyeBInner,
            mouthCornerLeft to mouthCornerRight
        )
        var sumX = 0.0
        var sumY = 0.0
        for ((l, r) in pairs) {
            val dl = abs(midX - l.x) * scaleA
            val dr = abs(r.x - midX) * scaleB
            val dx = (abs(dl - dr) / interocular).toDouble()
            val dy = (abs(l.y - r.y) / faceHeight).toDouble()
            sumX += dx * dx
            sumY += dy * dy
        }
        val count = pairs.size.coerceAtLeast(1)
        val symmetryRms = (0.75 * sqrt(sumX / count) + 0.25 * sqrt(sumY / count)).toFloat()
        val eyeWidthAsymmetry = abs(eyeWidthA - eyeWidthB) / eyeWidth

        val jawY = chinY - 0.18f * faceHeight
        val (jawLeft, jawRight) = horizontalSpan(face, jawY, 0.03f * faceHeight, faceWidth)
        val jawWidth = max(jawRight - jawLeft, EPS)

        // 颏角：下巴尖与左右下颌点构成的夹角（V 形下巴更小，圆润下巴更大）。
        val chinSideY = chinY - 0.10f * faceHeight
        val (chinLeft, chinRight) = horizontalSpan(face, chinSideY, 0.03f * faceHeight, faceWidth)
        val chinAngle = angleDegrees(Pt(chinLeft, chinSideY), chinPoint, Pt(chinRight, chinSideY))

        val eyeAspect = (eyeHeightA / max(eyeWidthA, EPS) + eyeHeightB / max(eyeWidthB, EPS)) / 2f
        // eyeA 在图像左侧（外眼角为 min-x），eyeB 在图像右侧（外眼角为 max-x）。
        val canthalTilt = (canthalTilt(eyeAInner, eyeAOuter) + canthalTilt(eyeBInner, eyeBOuter)) / 2f

        return FaceMeasurements(
            faceHeight = faceHeight,
            faceWidth = faceWidth,
            eyeLevelWidth = eyeLevelWidth,
            thirds = thirds,
            fiveEyes = fiveEyes,
            eyeWidths = listOf(eyeWidthA, eyeWidthB),
            eyeHeights = listOf(eyeHeightA, eyeHeightB),
            eyeCenters = listOf(eyeCA, eyeCB),
            browEyeGap = max(eyeCenter.y - browY, 0f),
            browEyeGapRatio = max(eyeCenter.y - browY, 0f) / eyeHeight,
            noseWidth = noseWidth,
            noseWidthToEye = noseWidth / eyeWidth,
            noseWidthToMouth = noseWidth / mouthWidth,
            noseWidthToFace = noseWidth / faceWidth,
            noseLengthNorm = max(subnasaleY - browY, 0f) / faceHeight,
            mouthWidth = mouthWidth,
            mouthWidthToFace = mouthWidth / faceWidth,
            upperLowerLipRatio = upperLipH / max(lowerLipH, EPS),
            lipHeightToMouth = lipHeight / mouthWidth,
            mouthTiltDeg = mouthTilt,
            symmetryRms = symmetryRms,
            eyeWidthAsymmetry = eyeWidthAsymmetry,
            jawWidthToFace = jawWidth / faceWidth,
            chinAngleDeg = chinAngle,
            jawWidthToHeight = jawWidth / faceHeight,
            eyeAspect = eyeAspect,
            canthalTiltDeg = canthalTilt,
            interocularToEyeWidth = interocular / eyeWidth,
            foreheadToBrow = max(browY - topY, 0f) / faceHeight,
            smileProbability = lm.smilingProbability,
            eyeOpenProbability = lm.leftEyeOpenProbability?.let { l ->
                lm.rightEyeOpenProbability?.let { r -> (l + r) / 2f } ?: l
            },
            poseYaw = lm.yawDeg,
            posePitch = lm.pitchDeg,
            contourPoints = lm.contourCount()
        )
    }

    /** 眼角倾斜：外眼角高于内眼角时为正（单位：度）。 */
    private fun canthalTilt(inner: Pt, outer: Pt): Float {
        val dx = abs(inner.x - outer.x)
        if (dx < EPS) return 0f
        return Math.toDegrees(atan2((inner.y - outer.y).toDouble(), dx.toDouble())).toFloat()
    }

    private fun horizontalExtent(points: List<Pt>): Float {
        if (points.isEmpty()) return 0f
        return points.maxOf { it.x } - points.minOf { it.x }
    }

    private fun verticalExtent(points: List<Pt>): Float {
        if (points.isEmpty()) return 0f
        return points.maxOf { it.y } - points.minOf { it.y }
    }

    private fun meanYInMiddle(points: List<Pt>): Float {
        if (points.isEmpty()) return 0f
        val sorted = points.sortedBy { it.x }
        val from = (sorted.size * 0.25f).toInt().coerceIn(0, sorted.size - 1)
        val to = (sorted.size * 0.75f).toInt().coerceIn(from + 1, sorted.size)
        return sorted.subList(from, to).map { it.y }.average().toFloat()
    }

    /**
     * 求人脸轮廓在给定高度处的水平跨度。
     * 先在 ±band 内找左右极值，找不到则逐步放宽；仍失败时取 y 最近的若干点。
     */
    fun horizontalSpan(points: List<Pt>, y: Float, band: Float, faceWidth: Float): Pair<Float, Float> {
        var b = band
        repeat(4) {
            var left = Float.MAX_VALUE
            var right = -Float.MAX_VALUE
            for (p in points) {
                if (abs(p.y - y) <= b) {
                    left = min(left, p.x)
                    right = max(right, p.x)
                }
            }
            if (left < Float.MAX_VALUE && right > -Float.MAX_VALUE && right - left > 0.05f * faceWidth) {
                return left to right
            }
            b *= 2.5f
        }
        val nearest = points.sortedBy { abs(it.y - y) }.take(8)
        if (nearest.isEmpty()) return 0f to 0f
        return nearest.minOf { it.x } to nearest.maxOf { it.x }
    }

    /** 用于结果页绘制：把人脸轮廓点裁剪到归一化图像范围内。 */
    fun clampBox(box: Box): Box = Box(
        box.left.coerceIn(0f, 1f),
        box.top.coerceIn(0f, 1f),
        box.right.coerceIn(0f, 1f),
        box.bottom.coerceIn(0f, 1f)
    )

    fun distance(a: Pt, b: Pt): Float = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
}
