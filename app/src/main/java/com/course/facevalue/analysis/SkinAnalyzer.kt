package com.course.facevalue.analysis

import android.graphics.Bitmap
import android.graphics.Color
import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.Pt
import com.course.facevalue.analysis.model.centroid
import kotlin.math.sqrt

/**
 * 肤质统计：在双颊与额头划定椭圆采样区，统计亮度/色度的离散程度。
 * 只反映“拍摄光照 + 肤色一致性”的混合结果，不能替代皮肤检测。
 */
object SkinAnalyzer {

    fun fromBitmap(bitmap: Bitmap, lm: FaceLandmarks): SkinStats? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null
        val box = lm.bounds
        if (box.width <= 0f || box.height <= 0f) return null

        val cheek1 = if (lm.leftCheek.isEmpty()) null else lm.leftCheek.centroid()
        val cheek2 = if (lm.rightCheek.isEmpty()) null else lm.rightCheek.centroid()
        val centers = buildList {
            if (cheek1 != null) add(cheek1)
            if (cheek2 != null) add(cheek2)
            if (size < 2) {
                add(Pt(box.centerX - 0.16f * box.width, box.top + 0.58f * box.height))
                add(Pt(box.centerX + 0.16f * box.width, box.top + 0.58f * box.height))
            }
            add(Pt(box.centerX, box.top + 0.15f * box.height))
        }

        val radius = (0.055f * box.width * w).coerceAtLeast(5f)
        val pixels = ArrayList<Int>(4096)
        for (center in centers) {
            val cx = (center.x * w).toInt()
            val cy = (center.y * h).toInt()
            val r = radius.toInt()
            for (dy in -r..r) {
                for (dx in -r..r) {
                    if ((dx * dx + dy * dy).toFloat() > radius * radius) continue
                    val x = cx + dx
                    val y = cy + dy
                    if (x in 0 until w && y in 0 until h) {
                        pixels += bitmap.getPixel(x, y)
                    }
                }
            }
        }
        if (pixels.size < 50) return null

        var sumL = 0.0
        var sumL2 = 0.0
        var sumA = 0.0
        var sumA2 = 0.0
        var sumB = 0.0
        var sumB2 = 0.0
        var highlight = 0
        var counted = 0
        for (c in pixels) {
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val lum = 0.299 * r + 0.587 * g + 0.114 * b
            val lab = rgbToLab(r, g, b)
            sumL += lab[0]; sumL2 += lab[0] * lab[0]
            sumA += lab[1]; sumA2 += lab[1] * lab[1]
            sumB += lab[2]; sumB2 += lab[2] * lab[2]
            if (lum > 242) highlight++
            counted++
        }
        if (counted == 0) return null
        val meanL = sumL / counted
        val varL = (sumL2 / counted - meanL * meanL).coerceAtLeast(0.0)
        val meanA = sumA / counted
        val varA = (sumA2 / counted - meanA * meanA).coerceAtLeast(0.0)
        val meanB = sumB / counted
        val varB = (sumB2 / counted - meanB * meanB).coerceAtLeast(0.0)

        return SkinStats(
            lumaStd = sqrt(varL).toFloat(),
            chromaStd = sqrt((varA + varB) / 2.0).toFloat(),
            highlightRatio = highlight.toFloat() / counted,
            meanLuma = (meanL / 100.0 * 255.0).toFloat(),
            sampleCount = counted,
            fromColor = true
        )
    }

    /** sRGB → CIELAB（D65）。返回 L(0..100)、a、b。 */
    fun rgbToLab(r: Int, g: Int, b: Int): DoubleArray {
        val rl = srgbToLinear(r / 255.0)
        val gl = srgbToLinear(g / 255.0)
        val bl = srgbToLinear(b / 255.0)
        val x = (0.4124 * rl + 0.3576 * gl + 0.1805 * bl) / 0.95047
        val y = (0.2126 * rl + 0.7152 * gl + 0.0722 * bl) / 1.00000
        val z = (0.0193 * rl + 0.1192 * gl + 0.9505 * bl) / 1.08883
        val fx = labF(x)
        val fy = labF(y)
        val fz = labF(z)
        return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    private fun srgbToLinear(c: Double): Double =
        if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)

    private fun labF(t: Double): Double =
        if (t > 0.008856) Math.pow(t, 1.0 / 3.0) else (7.787 * t + 16.0 / 116.0)
}
