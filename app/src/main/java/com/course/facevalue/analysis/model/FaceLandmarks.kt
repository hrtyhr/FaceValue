package com.course.facevalue.analysis.model

/**
 * 一张人脸的轮廓关键点集合（归一化坐标）。
 *
 * 数据来源：ML Kit Face Detection 的 13 组 face contour，合计 130+ 个点，
 * 在本项目中按语义分组，并重采样为 68 点 / 106 点标准布局用于展示。
 *
 * left/right 采用 ML Kit 语义（受试者视角），几何计算前会按图像 x 重新排序，
 * 因此不依赖左右语义。
 */
data class FaceLandmarks(
    val face: List<Pt>,
    val leftBrow: List<Pt>,
    val rightBrow: List<Pt>,
    val leftEye: List<Pt>,
    val rightEye: List<Pt>,
    val noseBridge: List<Pt>,
    val noseBottom: List<Pt>,
    val upperLipTop: List<Pt>,
    val upperLipBottom: List<Pt>,
    val lowerLipTop: List<Pt>,
    val lowerLipBottom: List<Pt>,
    val leftCheek: List<Pt>,
    val rightCheek: List<Pt>,
    val bounds: Box,
    val rollDeg: Float,
    val yawDeg: Float,
    val pitchDeg: Float,
    val smilingProbability: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val trackingId: Int? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val detectedPointCount: Int = 0
) {
    val points: List<Pt> get() = face + leftBrow + rightBrow + leftEye + rightEye + noseBridge +
        noseBottom + upperLipTop + upperLipBottom + lowerLipTop + lowerLipBottom + leftCheek + rightCheek

    fun contourCount(): Int = if (detectedPointCount > 0) detectedPointCount else points.size

    fun hasCoreContours(): Boolean =
        face.size >= 8 && leftEye.size >= 4 && rightEye.size >= 4 &&
            leftBrow.isNotEmpty() && rightBrow.isNotEmpty() &&
            noseBottom.size >= 2 && upperLipTop.size >= 3 && upperLipBottom.size >= 3 &&
            lowerLipTop.size >= 3 && lowerLipBottom.size >= 3
}

/** 人脸的几何测量结果；除角度外均已归一化（除以脸宽或脸高）。 */
data class FaceMeasurements(
    val faceHeight: Float,
    val faceWidth: Float,
    val eyeLevelWidth: Float,
    val thirds: List<Float>,
    val fiveEyes: List<Float>,
    val eyeWidths: List<Float>,
    val eyeHeights: List<Float>,
    val eyeCenters: List<Pt>,
    val browEyeGap: Float,
    val browEyeGapRatio: Float,
    val noseWidth: Float,
    val noseWidthToEye: Float,
    val noseWidthToMouth: Float,
    val noseWidthToFace: Float,
    val noseLengthNorm: Float,
    val mouthWidth: Float,
    val mouthWidthToFace: Float,
    val upperLowerLipRatio: Float,
    val lipHeightToMouth: Float,
    val mouthTiltDeg: Float,
    val symmetryRms: Float,
    val eyeWidthAsymmetry: Float,
    val jawWidthToFace: Float,
    val chinAngleDeg: Float,
    val jawWidthToHeight: Float,
    val eyeAspect: Float,
    val canthalTiltDeg: Float,
    val interocularToEyeWidth: Float,
    val foreheadToBrow: Float,
    val smileProbability: Float?,
    val eyeOpenProbability: Float?,
    val poseYaw: Float,
    val posePitch: Float,
    val contourPoints: Int
)
