package com.course.facevalue.analysis

import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.Pt
import kotlin.math.hypot

/**
 * 68 / 106 点标准布局。
 *
 * ML Kit 返回的是沿轮廓的密集点，本项目按弧长等距重采样到固定点数：
 *  - 68 点：接近 iBUG 300-W 的分组（下颌 17、眉 5+5、鼻 9、眼 6+6、唇 20）；
 *  - 106 点：面部轮廓 33、眉 8+8、眼 8+8、鼻 9、外唇 20、内唇 12。
 */
object KeypointLayout {

    data class LayoutPoint(val point: Pt, val group: String)

    fun layout68(lm: FaceLandmarks): List<LayoutPoint> = build(
        lm,
        faceCount = 17,
        browCount = 5,
        eyeCount = 6,
        noseCount = 9,
        outerLipCount = 12,
        innerLipCount = 8
    )

    fun layout106(lm: FaceLandmarks): List<LayoutPoint> = build(
        lm,
        faceCount = 33,
        browCount = 8,
        eyeCount = 8,
        noseCount = 9,
        outerLipCount = 20,
        innerLipCount = 12
    )

    private fun build(
        lm: FaceLandmarks,
        faceCount: Int,
        browCount: Int,
        eyeCount: Int,
        noseCount: Int,
        outerLipCount: Int,
        innerLipCount: Int
    ): List<LayoutPoint> {
        val out = mutableListOf<LayoutPoint>()
        out += resample(lm.face, faceCount).map { LayoutPoint(it, "face") }
        out += resample(lm.leftBrow, browCount).map { LayoutPoint(it, "brow") }
        out += resample(lm.rightBrow, browCount).map { LayoutPoint(it, "brow") }
        out += resample(lm.leftEye, eyeCount).map { LayoutPoint(it, "eye") }
        out += resample(lm.rightEye, eyeCount).map { LayoutPoint(it, "eye") }
        out += resample(lm.noseBridge + lm.noseBottom, noseCount).map { LayoutPoint(it, "nose") }
        out += resample(lm.upperLipTop, outerLipCount / 2).map { LayoutPoint(it, "lip") }
        out += resample(lm.lowerLipBottom, outerLipCount - outerLipCount / 2)
            .map { LayoutPoint(it, "lip") }
        out += resample(lm.upperLipBottom, innerLipCount / 2).map { LayoutPoint(it, "innerLip") }
        out += resample(lm.lowerLipTop, innerLipCount - innerLipCount / 2)
            .map { LayoutPoint(it, "innerLip") }
        return out
    }

    /** 按累计弧长等距重采样折线；点数不足时重复端点。 */
    fun resample(points: List<Pt>, count: Int): List<Pt> {
        if (count <= 0) return emptyList()
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return List(count) { points[0] }
        val clean = mutableListOf<Pt>()
        for (p in points) {
            if (clean.isEmpty() || hypot(
                    (p.x - clean.last().x).toDouble(),
                    (p.y - clean.last().y).toDouble()
                ) > 1e-6
            ) clean += p
        }
        if (clean.size == 1) return List(count) { clean[0] }
        val cumulative = DoubleArray(clean.size)
        for (i in 1 until clean.size) {
            cumulative[i] = cumulative[i - 1] + hypot(
                (clean[i].x - clean[i - 1].x).toDouble(),
                (clean[i].y - clean[i - 1].y).toDouble()
            )
        }
        val total = cumulative.last()
        if (total < 1e-9) return List(count) { clean[0] }
        val result = ArrayList<Pt>(count)
        var seg = 1
        for (i in 0 until count) {
            val target = total * i / (count - 1).coerceAtLeast(1)
            while (seg < clean.size - 1 && cumulative[seg] < target) seg++
            val segLen = cumulative[seg] - cumulative[seg - 1]
            val t = if (segLen <= 1e-9) 0.0 else (target - cumulative[seg - 1]) / segLen
            val a = clean[seg - 1]
            val b = clean[seg]
            result += Pt(
                (a.x + (b.x - a.x) * t).toFloat(),
                (a.y + (b.y - a.y) * t).toFloat()
            )
        }
        return result
    }
}
