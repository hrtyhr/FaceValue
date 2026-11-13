package com.course.facevalue

import com.course.facevalue.analysis.FeatureKey
import com.course.facevalue.analysis.GenderProfile
import com.course.facevalue.analysis.GeometryCalculator
import com.course.facevalue.analysis.KeypointLayout
import com.course.facevalue.analysis.ScoreScale
import com.course.facevalue.analysis.ScoringEngine
import com.course.facevalue.analysis.SkinStats
import com.course.facevalue.analysis.model.Box
import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GeometryScoringTest {

    private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, count: Int, start: Float = 0f): List<Pt> =
        (0 until count).map { i ->
            val t = start + 2f * PI.toFloat() * i / count
            Pt(cx + rx * cos(t), cy + ry * sin(t))
        }

    private fun arc(cx: Float, cy: Float, rx: Float, ry: Float, count: Int, from: Float, to: Float): List<Pt> =
        (0 until count).map { i ->
            val t = from + (to - from) * i / (count - 1)
            Pt(cx + rx * cos(t), cy + ry * sin(t))
        }

    /** 以标准比例构造的“模板脸”，用于回归测试。 */
    private fun templateFace(eyeShift: Float = 0f): FaceLandmarks {
        val cx = 0.5f
        val topY = 0.22f
        val chinY = 0.70f
        val h = chinY - topY
        val w = 0.34f
        val cy = (topY + chinY) / 2f
        val browY = topY + h / 3f
        val noseBaseY = topY + 2f * h / 3f
        val eyeY = browY + 0.035f
        val eyeDx = 0.0715f
        val eyeRx = 0.036f
        val eyeRy = 0.015f
        val mouthY = noseBaseY + 0.055f

        val face = ellipse(cx, cy, w / 2f, h / 2f, 40, 0f).sortedBy { it.y }
        val leftEye = ellipse(cx - eyeDx, eyeY + eyeShift, eyeRx, eyeRy, 12)
        val rightEye = ellipse(cx + eyeDx, eyeY, eyeRx, eyeRy, 12)
        val leftBrow = arc(cx - eyeDx, eyeY - 0.029f, 0.045f, 0.010f, 10, PI.toFloat(), 2 * PI.toFloat())
        val rightBrow = arc(cx + eyeDx, eyeY - 0.029f, 0.045f, 0.010f, 10, PI.toFloat(), 2 * PI.toFloat())
        val noseBridge = (0 until 6).map { i ->
            Pt(cx, browY + (noseBaseY - 0.02f - browY) * i / 5f)
        }
        val noseBottom = ellipse(cx, noseBaseY, 0.030f, 0.010f, 10)
        val mouthRx = 0.052f
        val mouthRy = 0.016f
        val upperTop = arc(cx, mouthY, mouthRx, mouthRy, 11, PI.toFloat(), 2 * PI.toFloat())
        val upperBottom = arc(cx, mouthY, mouthRx * 0.92f, mouthRy * 0.7f, 9, PI.toFloat(), 2 * PI.toFloat())
        val lowerTop = arc(cx, mouthY, mouthRx * 0.92f, mouthRy * 0.7f, 9, 0f, PI.toFloat())
        val lowerBottom = arc(cx, mouthY, mouthRx, mouthRy * 1.25f, 11, 0f, PI.toFloat())
        val cheeks = listOf(Pt(cx - 0.12f, noseBaseY - 0.02f), Pt(cx + 0.12f, noseBaseY - 0.02f))
        return FaceLandmarks(
            face = face,
            leftBrow = leftBrow,
            rightBrow = rightBrow,
            leftEye = leftEye,
            rightEye = rightEye,
            noseBridge = noseBridge,
            noseBottom = noseBottom,
            upperLipTop = upperTop,
            upperLipBottom = upperBottom,
            lowerLipTop = lowerTop,
            lowerLipBottom = lowerBottom,
            leftCheek = listOf(cheeks[0]),
            rightCheek = listOf(cheeks[1]),
            bounds = Box(cx - w / 2f, topY, cx + w / 2f, chinY),
            rollDeg = 0f,
            yawDeg = 0f,
            pitchDeg = 0f,
            smilingProbability = 0.4f,
            leftEyeOpenProbability = 0.9f,
            rightEyeOpenProbability = 0.9f,
            imageWidth = 1000,
            imageHeight = 1000
        )
    }

    /** 双眼外眼角抬高，用于验证眼角倾斜符号。 */
    private fun templateFaceWithUpturnedEyes(k: Float = 0.003f): FaceLandmarks {
        val base = templateFace()
        fun tilt(points: List<Pt>, sign: Float): List<Pt> {
            if (points.isEmpty()) return points
            val cx = points.map { it.x }.average().toFloat()
            val rx = (points.maxOf { it.x } - points.minOf { it.x }) / 2f
            if (rx <= 0f) return points
            return points.map { Pt(it.x, it.y + sign * k * (it.x - cx) / rx) }
        }
        return base.copy(
            leftEye = tilt(base.leftEye, 1f),
            rightEye = tilt(base.rightEye, -1f)
        )
    }

    /** 围绕画面中心等比缩放整张脸，模拟同一张脸在不同拍摄距离下占画面的比例。 */
    private fun FaceLandmarks.scaledAroundCenter(factor: Float): FaceLandmarks {
        val cx = 0.5f
        val cy = 0.5f
        fun map(p: Pt) = Pt(cx + (p.x - cx) * factor, cy + (p.y - cy) * factor)
        return copy(
            face = face.map(::map),
            leftBrow = leftBrow.map(::map),
            rightBrow = rightBrow.map(::map),
            leftEye = leftEye.map(::map),
            rightEye = rightEye.map(::map),
            noseBridge = noseBridge.map(::map),
            noseBottom = noseBottom.map(::map),
            upperLipTop = upperLipTop.map(::map),
            upperLipBottom = upperLipBottom.map(::map),
            lowerLipTop = lowerLipTop.map(::map),
            lowerLipBottom = lowerLipBottom.map(::map),
            leftCheek = leftCheek.map(::map),
            rightCheek = rightCheek.map(::map),
            bounds = Box(
                cx + (bounds.left - cx) * factor,
                cy + (bounds.top - cy) * factor,
                cx + (bounds.right - cx) * factor,
                cy + (bounds.bottom - cy) * factor
            )
        )
    }

    @Test
    fun templateFaceHasBalancedThirds() {
        val m = GeometryCalculator.measure(templateFace())!!
        assertEquals(1f / 3f, m.thirds[0], 0.03f)
        assertEquals(1f / 3f, m.thirds[1], 0.03f)
        assertEquals(1f / 3f, m.thirds[2], 0.03f)
        assertTrue(m.thirds.sum() > 0.99f)
    }

    @Test
    fun templateFaceIsNearlySymmetric() {
        val m = GeometryCalculator.measure(templateFace())!!
        assertTrue("symmetry rms=${m.symmetryRms}", m.symmetryRms < 0.01f)
        val s = ScoringEngine.score(m, null, GenderProfile.NEUTRAL)
        val sym = s.features.first { it.key == FeatureKey.SYMMETRY }
        assertTrue("symmetry score=${sym.score}", sym.score > 0.8f)
    }

    @Test
    fun eyeShiftLowersSymmetryScore() {
        val base = GeometryCalculator.measure(templateFace())!!
        val shifted = GeometryCalculator.measure(templateFace(eyeShift = 0.018f))!!
        assertTrue(shifted.symmetryRms > base.symmetryRms)
        val baseScore = ScoringEngine.score(base, null, GenderProfile.NEUTRAL)
            .features.first { it.key == FeatureKey.SYMMETRY }.score
        val shiftedScore = ScoringEngine.score(shifted, null, GenderProfile.NEUTRAL)
            .features.first { it.key == FeatureKey.SYMMETRY }.score
        assertTrue("$shiftedScore should be < $baseScore", shiftedScore < baseScore)
    }

    @Test
    fun canthalTiltIsPositiveForUpturnedEyes() {
        val m = GeometryCalculator.measure(templateFaceWithUpturnedEyes())!!
        assertTrue("canthal tilt=${m.canthalTiltDeg}", m.canthalTiltDeg > 3f)
        assertTrue("canthal tilt=${m.canthalTiltDeg}", m.canthalTiltDeg < 7f)
    }

    @Test
    fun browEyeGapScoreIsFramingInvariant() {
        val large = GeometryCalculator.measure(templateFace())!!
        val small = GeometryCalculator.measure(templateFace().scaledAroundCenter(0.6f))!!
        fun browScore(m: com.course.facevalue.analysis.model.FaceMeasurements): Float =
            ScoringEngine.score(m, null, GenderProfile.NEUTRAL)
                .features.first { it.key == FeatureKey.BROW_EYE }.score
        assertEquals(browScore(large), browScore(small), 1e-4f)
    }

    @Test
    fun missingBrowOrInnerLipContoursAreRejected() {
        assertNull(GeometryCalculator.measure(templateFace().copy(leftBrow = emptyList())))
        assertNull(GeometryCalculator.measure(templateFace().copy(upperLipBottom = emptyList())))
    }

    @Test
    fun weightsSumToOne() {
        GenderProfile.entries.forEach { profile ->
            val sum = profile.weights.values.sum()
            assertEquals(profile.name, 1f, sum, 0.001f)
        }
    }

    @Test
    fun genderProfilesProduceDifferentScores() {
        val m = GeometryCalculator.measure(templateFace())!!
        val male = ScoringEngine.score(m, null, GenderProfile.MALE)
        val female = ScoringEngine.score(m, null, GenderProfile.FEMALE)
        assertTrue(male.features.size == 10 && female.features.size == 10)
        assertTrue(male.raw in 0f..1f && female.raw in 0f..1f)
    }

    @Test
    fun keypointLayoutsHaveExpectedCounts() {
        val lm = templateFace()
        assertEquals(68, KeypointLayout.layout68(lm).size)
        assertEquals(106, KeypointLayout.layout106(lm).size)
        assertEquals(17, KeypointLayout.layout68(lm).count { it.group == "face" })
    }

    @Test
    fun resampleKeepsEndpointsAndCount() {
        val line = listOf(Pt(0f, 0f), Pt(1f, 0f), Pt(1f, 1f))
        val out = KeypointLayout.resample(line, 9)
        assertEquals(9, out.size)
        assertEquals(0f, out.first().x, 1e-5f)
        assertEquals(1f, out.last().y, 1e-5f)
    }

    @Test
    fun scoreScaleMapsCorrectly() {
        assertEquals(10f, ScoreScale.TEN.display(1f), 1e-4f)
        assertEquals(1f, ScoreScale.TEN.display(0f), 1e-4f)
        assertEquals(5f, ScoreScale.FIVE.display(1f), 1e-4f)
        assertEquals("10.0", ScoreScale.TEN.format(1f))
        assertEquals("5.0", ScoreScale.FIVE.format(1f))
    }

    @Test
    fun skinQualityAffectsSkinFeature() {
        val m = GeometryCalculator.measure(templateFace())!!
        val even = SkinStats(lumaStd = 2f, chromaStd = 2f, highlightRatio = 0.01f, meanLuma = 180f, sampleCount = 500, fromColor = true)
        val uneven = SkinStats(lumaStd = 22f, chromaStd = 16f, highlightRatio = 0.2f, meanLuma = 180f, sampleCount = 500, fromColor = true)
        val a = ScoringEngine.score(m, even, GenderProfile.NEUTRAL).features.first { it.key == FeatureKey.SKIN }.score
        val b = ScoringEngine.score(m, uneven, GenderProfile.NEUTRAL).features.first { it.key == FeatureKey.SKIN }.score
        assertTrue("$a should be > $b", a > b)
    }
}
