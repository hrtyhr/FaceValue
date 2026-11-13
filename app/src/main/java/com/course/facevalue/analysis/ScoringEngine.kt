package com.course.facevalue.analysis

import com.course.facevalue.analysis.model.FaceMeasurements
import kotlin.math.exp

/** 肤质统计；lumaStd / chromaStd 为 0..255 亮度、色度标准差。 */
data class SkinStats(
    val lumaStd: Float,
    val chromaStd: Float?,
    val highlightRatio: Float,
    val meanLuma: Float,
    val sampleCount: Int,
    val fromColor: Boolean
)

enum class ScoreScale(val max: Int, val label: String) {
    TEN(10, "10 分制"),
    FIVE(5, "5 分制");

    fun display(raw01: Float): Float = 1f + (max - 1) * raw01.coerceIn(0f, 1f)

    fun format(raw01: Float): String {
        val v = display(raw01)
        return String.format("%.1f", v)
    }
}

enum class FeatureKey(val label: String, val idealHint: String) {
    SYMMETRY("面部对称性", "左右眼与嘴角到中线的距离接近"),
    THIRDS("三庭比例", "上庭 : 中庭 : 下庭 ≈ 1 : 1 : 1"),
    FIVE_EYES("五眼比例", "面部宽度 ≈ 5 个眼宽"),
    BROW_EYE("眉眼间距", "眉眼距 ≈ 2 倍眼高"),
    NOSE("鼻部比例", "鼻宽 ≈ 1.25 倍眼宽 ≈ 0.63 倍唇宽"),
    LIPS("唇形比例", "唇宽 ≈ 0.41 倍面宽，上/下唇 ≈ 0.6"),
    CONTOUR("轮廓与下颌", "下颌宽与颏角符合当前偏好预设"),
    EYE_SHAPE("眼型", "眼高宽比与眼角倾斜在参考区间"),
    SKIN("肤色均匀度", "面部亮度分布越均匀越好"),
    EXPRESSION("表情自然度", "自然微笑或中性表情，双眼睁开")
}

/** 分性别权重：仅代表不同审美侧重点的“偏好预设”，不是生理或身份判断。 */
enum class GenderProfile(val label: String, val short: String, val note: String) {
    MALE(
        "男性", "男",
        "该预设提高了轮廓与下颌线（20%）、对称性（18%）的权重，眉眼结构次之，唇形权重较低。"
    ),
    FEMALE(
        "女性", "女",
        "该预设提高了唇形（14%）、眼型（12%）与面部柔和度的权重，轮廓仍保留 10%。"
    ),
    NEUTRAL(
        "通用", "中",
        "该预设对各项指标取折中权重，不强调任一性别的统计偏好。"
    );

    val weights: Map<FeatureKey, Float>
        get() = when (this) {
            MALE -> mapOf(
                FeatureKey.SYMMETRY to 0.18f,
                FeatureKey.THIRDS to 0.10f,
                FeatureKey.FIVE_EYES to 0.12f,
                FeatureKey.BROW_EYE to 0.10f,
                FeatureKey.NOSE to 0.10f,
                FeatureKey.LIPS to 0.06f,
                FeatureKey.CONTOUR to 0.20f,
                FeatureKey.EYE_SHAPE to 0.06f,
                FeatureKey.SKIN to 0.05f,
                FeatureKey.EXPRESSION to 0.03f
            )
            FEMALE -> mapOf(
                FeatureKey.SYMMETRY to 0.18f,
                FeatureKey.THIRDS to 0.12f,
                FeatureKey.FIVE_EYES to 0.12f,
                FeatureKey.BROW_EYE to 0.08f,
                FeatureKey.NOSE to 0.08f,
                FeatureKey.LIPS to 0.14f,
                FeatureKey.CONTOUR to 0.10f,
                FeatureKey.EYE_SHAPE to 0.12f,
                FeatureKey.SKIN to 0.04f,
                FeatureKey.EXPRESSION to 0.02f
            )
            NEUTRAL -> mapOf(
                FeatureKey.SYMMETRY to 0.18f,
                FeatureKey.THIRDS to 0.11f,
                FeatureKey.FIVE_EYES to 0.12f,
                FeatureKey.BROW_EYE to 0.09f,
                FeatureKey.NOSE to 0.09f,
                FeatureKey.LIPS to 0.10f,
                FeatureKey.CONTOUR to 0.15f,
                FeatureKey.EYE_SHAPE to 0.09f,
                FeatureKey.SKIN to 0.045f,
                FeatureKey.EXPRESSION to 0.025f
            )
        }

    /** 轮廓目标：男性偏好更清晰的下颌与更锐的颏角，女性偏好更柔和的过渡。 */
    val jawWidthTarget: Float get() = when (this) { MALE -> 0.79f; FEMALE -> 0.745f; NEUTRAL -> 0.77f }
    val chinAngleTarget: Float get() = when (this) { MALE -> 128f; FEMALE -> 136f; NEUTRAL -> 132f }
    val jawHeightTarget: Float get() = when (this) { MALE -> 0.60f; FEMALE -> 0.58f; NEUTRAL -> 0.59f }
    val eyeAspectTarget: Float get() = when (this) { MALE -> 0.285f; FEMALE -> 0.315f; NEUTRAL -> 0.30f }
    val canthalTiltTarget: Float get() = 0f
}

data class FeatureScore(
    val key: FeatureKey,
    val score: Float,
    val weight: Float,
    val measured: String,
    val comment: String
)

data class ScoreResult(
    val raw: Float,
    val confidence: Float,
    val features: List<FeatureScore>,
    val profile: GenderProfile,
    val highlights: List<String>,
    val suggestions: List<String>
) {
    fun display(scale: ScoreScale): Float = scale.display(raw)

    fun scoreText(scale: ScoreScale): String = scale.format(raw)

    val confidenceLabel: String
        get() = when {
            confidence >= 0.75f -> "高"
            confidence >= 0.5f -> "中"
            else -> "低"
        }

    val grade: String
        get() = when {
            raw >= 0.86f -> "S · 几何高度协调"
            raw >= 0.78f -> "A · 协调"
            raw >= 0.70f -> "B · 较协调"
            raw >= 0.62f -> "C · 一般"
            else -> "D · 偏离参考区间"
        }
}

/**
 * 规则化评分引擎：把几何测量映射为 0..1 的子分数，再按性别偏好权重加权。
 *
 * 每个子分数使用高斯型隶属函数 exp(-0.5*((x-target)/sigma)^2)，
 * 表示测量值偏离参考区间中心的程度；这些参考区间来自面部比例的人类测量学
 * 常用近似值，并非“客观美丑”标准。
 */
object ScoringEngine {

    private fun gauss(x: Float, target: Float, sigma: Float): Float {
        val z = ((x - target) / sigma).toDouble()
        return exp(-0.5 * z * z).toFloat()
    }

    private fun band(x: Float, low: Float, high: Float, soft: Float): Float = when {
        x < low -> gauss(x, low, soft)
        x > high -> gauss(x, high, soft)
        else -> 1f
    }

    private fun fmt(v: Float, digits: Int = 2): String = String.format("%.${digits}f", v)

    fun score(m: FaceMeasurements, skin: SkinStats?, profile: GenderProfile): ScoreResult {
        val thirds = m.thirds
        val sThirds = thirds.map { gauss(it, 1f / 3f, 0.04f) }.average().toFloat()

        val five = m.fiveEyes
        val meanFive = five.average().toFloat().coerceAtLeast(1e-3f)
        val cv = kotlin.math.sqrt(five.map { (it - meanFive) * (it - meanFive) }.average().toFloat()) / meanFive
        val sFiveCv = gauss(cv, 0f, 0.13f)
        val sFiveWidth = gauss(five[1], 0.2f, 0.03f)
        val sFive = 0.55f * sFiveCv + 0.45f * sFiveWidth

        val sBrowGap = gauss(m.browEyeGapRatio, 2.1f, 0.38f)
        // browEyeGap 是相对图像尺寸的归一化距离，需除以脸高后才能与画面缩放无关。
        val sBrowNorm = gauss(m.browEyeGap / m.faceHeight, 0.072f, 0.02f)
        val sBrow = 0.6f * sBrowGap + 0.4f * sBrowNorm

        val sNose = 0.4f * gauss(m.noseWidthToEye, 1.25f, 0.16f) +
            0.35f * gauss(m.noseWidthToMouth, 0.63f, 0.07f) +
            0.25f * gauss(m.noseWidthToFace, 0.257f, 0.028f)

        val sLips = 0.4f * gauss(m.mouthWidthToFace, 0.41f, 0.04f) +
            0.3f * gauss(m.upperLowerLipRatio, 0.60f, 0.12f) +
            0.2f * gauss(m.lipHeightToMouth, 0.25f, 0.05f) +
            0.1f * gauss(m.mouthTiltDeg, 0f, 7f)

        val sContour = 0.45f * gauss(m.jawWidthToFace, profile.jawWidthTarget, 0.04f) +
            0.35f * gauss(m.chinAngleDeg, profile.chinAngleTarget, 11f) +
            0.20f * gauss(m.jawWidthToHeight, profile.jawHeightTarget, 0.05f)

        val sEye = 0.5f * gauss(m.eyeAspect, profile.eyeAspectTarget, 0.04f) +
            0.2f * gauss(m.canthalTiltDeg, profile.canthalTiltTarget, 3.5f) +
            0.3f * gauss(m.eyeWidths[0] / m.faceWidth, 0.195f, 0.025f)

        val sSym = 0.8f * gauss(m.symmetryRms, 0f, 0.03f) +
            0.2f * gauss(m.eyeWidthAsymmetry, 0f, 0.11f)

        val sSkin = if (skin == null || skin.sampleCount < 50) {
            0.62f
        } else {
            val sL = gauss(skin.lumaStd, 0f, 9.5f)
            val sC = if (skin.chromaStd != null && skin.fromColor) gauss(skin.chromaStd, 0f, 8f) else sL
            val sH = band(skin.highlightRatio, 0f, 0.06f, 0.06f)
            0.5f * sL + 0.3f * sC + 0.2f * sH
        }

        val sExpression = expressionScore(m)

        val rawFeatures = mapOf(
            FeatureKey.SYMMETRY to sSym,
            FeatureKey.THIRDS to sThirds,
            FeatureKey.FIVE_EYES to sFive,
            FeatureKey.BROW_EYE to sBrow,
            FeatureKey.NOSE to sNose,
            FeatureKey.LIPS to sLips,
            FeatureKey.CONTOUR to sContour,
            FeatureKey.EYE_SHAPE to sEye,
            FeatureKey.SKIN to sSkin,
            FeatureKey.EXPRESSION to sExpression
        )

        val weights = profile.weights
        var weighted = 0f
        var weightSum = 0f
        val features = rawFeatures.map { (key, s) ->
            val w = weights[key] ?: 0f
            weighted += w * s
            weightSum += w
            FeatureScore(
                key = key,
                score = s.coerceIn(0f, 1f),
                weight = w,
                measured = measuredText(key, m, skin),
                comment = commentFor(key, s, profile)
            )
        }.sortedByDescending { it.weight }

        val raw = if (weightSum > 0f) weighted / weightSum else 0f

        val sizeQuality = ((m.faceHeight - 0.12f) / 0.30f).coerceIn(0f, 1f)
        val poseQuality = exp(
            -0.5 * ((m.poseYaw / 14f) * (m.poseYaw / 14f) +
                (m.posePitch / 12f) * (m.posePitch / 12f)).toDouble()
        ).toFloat()
        val contourQuality = (m.contourPoints / 110f).coerceIn(0f, 1f)
        var confidence = 0.35f * sizeQuality + 0.35f * poseQuality + 0.30f * contourQuality
        if (kotlin.math.abs(m.poseYaw) > 28f || m.faceHeight < 0.14f) confidence *= 0.65f
        confidence = confidence.coerceIn(0.05f, 1f)

        val sorted = features.sortedByDescending { it.score }
        val highlights = sorted.filter { it.score >= 0.78f }.take(3).map { it.key.label }
        val suggestions = sorted.asReversed().take(2).map { it.key.label }

        return ScoreResult(
            raw = raw.coerceIn(0f, 1f),
            confidence = confidence,
            features = features,
            profile = profile,
            highlights = highlights,
            suggestions = suggestions
        )
    }

    private fun expressionScore(m: FaceMeasurements): Float {
        val smile = m.smileProbability ?: return 0.62f
        val eyeOpen = m.eyeOpenProbability ?: 0.9f
        val sSmile = band(smile, 0.05f, 0.90f, 0.15f)
        val sEyes = band(eyeOpen, 0.45f, 1f, 0.20f)
        return 0.6f * sSmile + 0.4f * sEyes
    }

    private fun measuredText(key: FeatureKey, m: FaceMeasurements, skin: SkinStats?): String = when (key) {
        FeatureKey.SYMMETRY -> "左右锚点偏差 ${fmt(m.symmetryRms * 100f, 1)}% 眼距"
        FeatureKey.THIRDS -> "上庭 ${fmt(m.thirds[0])} / 中庭 ${fmt(m.thirds[1])} / 下庭 ${fmt(m.thirds[2])}"
        FeatureKey.FIVE_EYES -> "五段 ${m.fiveEyes.joinToString(" : ") { fmt(it) }}"
        FeatureKey.BROW_EYE -> "眉眼距 ${fmt(m.browEyeGapRatio)}× 眼高"
        FeatureKey.NOSE -> "鼻宽/眼宽 ${fmt(m.noseWidthToEye)}，鼻宽/唇宽 ${fmt(m.noseWidthToMouth)}"
        FeatureKey.LIPS -> "唇宽/面宽 ${fmt(m.mouthWidthToFace)}，上/下唇 ${fmt(m.upperLowerLipRatio)}"
        FeatureKey.CONTOUR -> "下颌宽/面宽 ${fmt(m.jawWidthToFace)}，颏角 ${fmt(m.chinAngleDeg, 0)}°"
        FeatureKey.EYE_SHAPE -> "眼高宽比 ${fmt(m.eyeAspect)}，眼角倾斜 ${fmt(m.canthalTiltDeg, 1)}°"
        FeatureKey.SKIN -> if (skin == null || skin.sampleCount < 50) {
            "未获得足够的皮肤采样"
        } else {
            "亮度标准差 ${fmt(skin.lumaStd, 1)}，高光占比 ${fmt(skin.highlightRatio * 100f, 1)}%"
        }
        FeatureKey.EXPRESSION -> {
            val smile = m.smileProbability
            val eyes = m.eyeOpenProbability
            if (smile == null || eyes == null) {
                "未获得表情概率（按中性值处理）"
            } else {
                "微笑概率 ${fmt(smile)}，睁眼度 ${fmt(eyes)}"
            }
        }
    }

    private fun commentFor(key: FeatureKey, score: Float, profile: GenderProfile): String {
        val level = when {
            score >= 0.80f -> "good"
            score >= 0.62f -> "ok"
            else -> "low"
        }
        return when (key) {
            FeatureKey.SYMMETRY -> when (level) {
                "good" -> "左右眼与嘴角位置高度对称，视觉上更稳定。"
                "ok" -> "整体对称，局部（眼或嘴角）有轻微高低差。"
                else -> "左右差异较明显，可能由表情、角度或天生不对称造成。"
            }
            FeatureKey.THIRDS -> when (level) {
                "good" -> "上、中、下庭接近 1:1:1，纵向比例均衡（上庭以检测框顶边近似发际线）。"
                "ok" -> "三庭比例基本均衡，某一庭略长或略短。"
                else -> "三庭比例偏离较大；若刘海遮住额头，上庭会被低估。"
            }
            FeatureKey.FIVE_EYES -> when (level) {
                "good" -> "面部宽度约等于五只眼睛，眼距与两侧留白协调。"
                "ok" -> "五眼比例接近标准，眼距或眼宽有轻微偏差。"
                else -> "眼距或眼宽与“五眼”参考差异较大。"
            }
            FeatureKey.BROW_EYE -> when (level) {
                "good" -> "眉眼距离适中，眉眼结构舒展。"
                "ok" -> "眉眼间距略近或略远，整体仍协调。"
                else -> "眉眼间距与参考区间差异较大，可能受眉形或表情影响。"
            }
            FeatureKey.NOSE -> when (level) {
                "good" -> "鼻宽与眼宽、唇宽的比例协调。"
                "ok" -> "鼻部比例基本协调，鼻翼略宽或略窄。"
                else -> "鼻宽相对眼宽/唇宽偏离参考区间。"
            }
            FeatureKey.LIPS -> when (level) {
                "good" -> "唇宽与上下唇厚度比例均衡，唇形清晰。"
                "ok" -> "唇形比例接近参考，厚薄略有差异。"
                else -> "唇宽或上下唇比例偏离参考区间。"
            }
            FeatureKey.CONTOUR -> when (level) {
                "good" -> if (profile == GenderProfile.MALE) {
                    "下颌线清晰、颏部角度利落，轮廓结构感较强。"
                } else {
                    "下颌过渡柔和，面部轮廓圆润自然。"
                }
                "ok" -> "下颌宽度与颏角处于常见范围，轮廓自然。"
                else -> "下颌宽窄或颏角与当前偏好预设差异较大。"
            }
            FeatureKey.EYE_SHAPE -> when (level) {
                "good" -> "眼裂高度与眼角倾斜协调，眼型舒展。"
                "ok" -> "眼型接近参考区间，眼角倾斜略平或略上扬。"
                else -> "眼高宽比或眼角倾斜偏离参考区间（可能受眯眼影响）。"
            }
            FeatureKey.SKIN -> when (level) {
                "good" -> "面颊与额头亮度分布均匀，肤色一致性较好。"
                "ok" -> "肤色整体均匀，局部有轻微色差或高光。"
                else -> "采样区域亮度差异较大，可能受光照、油光或阴影影响。"
            }
            FeatureKey.EXPRESSION -> when (level) {
                "good" -> "表情自然，眉眼与嘴角放松。"
                "ok" -> "表情基本自然，轻微紧张或刻意微笑。"
                else -> "表情对几何测量有影响，建议放松面部后重拍。"
            }
        }
    }
}

