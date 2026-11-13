package com.course.facevalue

import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.course.facevalue.analysis.FaceDetectorWrapper
import com.course.facevalue.analysis.GenderProfile
import com.course.facevalue.analysis.GeometryCalculator
import com.course.facevalue.analysis.MlKitFaceMapper
import com.course.facevalue.analysis.ScoringEngine
import com.course.facevalue.analysis.SkinAnalyzer
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 批量实验：对 androidTest/assets/faces 下的合成人脸逐张执行
 * ML Kit 检测 → 关键点 → 几何测量 → 肤色统计 → 三种权重评分，
 * 输出 experiment.json 供课程设计报告做数据分析。
 */
@RunWith(AndroidJUnit4::class)
class BatchExperimentTest {

    @Test
    fun runBatchExperiment() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testAssets = instrumentation.context.assets
        val targetContext = instrumentation.targetContext
        val names = (testAssets.list("faces") ?: emptyArray())
            .filter { it.endsWith(".jpg", ignoreCase = true) }
            .sorted()

        assertTrue("测试图片缺失", names.isNotEmpty())

        val detector = FaceDetectorWrapper(accurate = true, tracking = false, minFaceSize = 0.08f)
        val results = JSONArray()
        try {
            for (name in names) {
                val bitmap = testAssets.open("faces/$name").use { BitmapFactory.decodeStream(it) }
                val started = SystemClock.elapsedRealtime()
                val faces = runBlocking { detector.detect(InputImage.fromBitmap(bitmap, 0)) }
                val detectMs = SystemClock.elapsedRealtime() - started

                val obj = JSONObject()
                    .put("name", name)
                    .put("width", bitmap.width)
                    .put("height", bitmap.height)
                    .put("detectMs", detectMs)
                    .put("faceCount", faces.size)

                val face = faces.maxByOrNull {
                    it.boundingBox.width().toLong() * it.boundingBox.height().toLong()
                }
                if (face == null) {
                    obj.put("error", "no_face")
                    results.put(obj)
                    continue
                }
                val landmarks = MlKitFaceMapper.toLandmarks(face, bitmap.width, bitmap.height)
                if (landmarks == null) {
                    obj.put("error", "no_landmarks")
                    results.put(obj)
                    continue
                }
                val m = GeometryCalculator.measure(landmarks)
                if (m == null) {
                    obj.put("error", "no_geometry")
                    results.put(obj)
                    continue
                }
                val skin = SkinAnalyzer.fromBitmap(bitmap, landmarks)

                obj.put("contourPoints", landmarks.contourCount())
                    .put("yaw", m.poseYaw.toDouble())
                    .put("pitch", m.posePitch.toDouble())
                    .put("faceHeightNorm", m.faceHeight.toDouble())
                    .put("thirds", JSONArray(m.thirds.map { it.toDouble() }))
                    .put("fiveEyes", JSONArray(m.fiveEyes.map { it.toDouble() }))
                    .put("symmetryRms", m.symmetryRms.toDouble())
                    .put("eyeWidthAsymmetry", m.eyeWidthAsymmetry.toDouble())
                    .put("browEyeGapRatio", m.browEyeGapRatio.toDouble())
                    .put("browEyeGapNorm", m.browEyeGap.toDouble())
                    .put("noseWidthToEye", m.noseWidthToEye.toDouble())
                    .put("noseWidthToMouth", m.noseWidthToMouth.toDouble())
                    .put("noseWidthToFace", m.noseWidthToFace.toDouble())
                    .put("mouthWidthToFace", m.mouthWidthToFace.toDouble())
                    .put("upperLowerLipRatio", m.upperLowerLipRatio.toDouble())
                    .put("lipHeightToMouth", m.lipHeightToMouth.toDouble())
                    .put("mouthTiltDeg", m.mouthTiltDeg.toDouble())
                    .put("jawWidthToFace", m.jawWidthToFace.toDouble())
                    .put("jawWidthToHeight", m.jawWidthToHeight.toDouble())
                    .put("chinAngleDeg", m.chinAngleDeg.toDouble())
                    .put("eyeAspect", m.eyeAspect.toDouble())
                    .put("canthalTiltDeg", m.canthalTiltDeg.toDouble())
                    .put("interocularToEyeWidth", m.interocularToEyeWidth.toDouble())
                    .put("smile", (m.smileProbability ?: -1f).toDouble())
                    .put("eyeOpen", (m.eyeOpenProbability ?: -1f).toDouble())

                if (skin != null) {
                    obj.put("lumaStd", skin.lumaStd.toDouble())
                        .put("chromaStd", (skin.chromaStd ?: -1f).toDouble())
                        .put("highlightRatio", skin.highlightRatio.toDouble())
                        .put("skinSamples", skin.sampleCount)
                }

                fun arr(points: List<com.course.facevalue.analysis.model.Pt>): JSONArray =
                    JSONArray(points.map { JSONArray(listOf(it.x.toDouble(), it.y.toDouble())) })
                obj.put("points", JSONObject().apply {
                    put("face", arr(landmarks.face))
                    put("leftEye", arr(landmarks.leftEye))
                    put("rightEye", arr(landmarks.rightEye))
                    put("leftBrow", arr(landmarks.leftBrow))
                    put("rightBrow", arr(landmarks.rightBrow))
                    put("noseBottom", arr(landmarks.noseBottom))
                    put("upperLipTop", arr(landmarks.upperLipTop))
                    put("lowerLipBottom", arr(landmarks.lowerLipBottom))
                    put("bounds", JSONArray(listOf(
                        landmarks.bounds.left.toDouble(), landmarks.bounds.top.toDouble(),
                        landmarks.bounds.right.toDouble(), landmarks.bounds.bottom.toDouble()
                    )))
                })

                val neutral = ScoringEngine.score(m, skin, GenderProfile.NEUTRAL)
                obj.put("featureScores", JSONObject().apply {
                    neutral.features.forEach { f -> put(f.key.name, f.score.toDouble()) }
                })
                obj.put("scoreNeutral", neutral.raw.toDouble())
                obj.put("confidence", neutral.confidence.toDouble())
                obj.put("scoreMale", ScoringEngine.score(m, skin, GenderProfile.MALE).raw.toDouble())
                obj.put("scoreFemale", ScoringEngine.score(m, skin, GenderProfile.FEMALE).raw.toDouble())
                obj.put("comments", JSONObject().apply {
                    neutral.features.forEach { f -> put(f.key.name, f.comment) }
                })
                results.put(obj)
                bitmap.recycle()
            }
        } finally {
            detector.close()
        }

        val out = File(targetContext.getExternalFilesDir(null), "experiment.json")
        out.writeText(results.toString(2))
        assertTrue("实验输出为空", out.length() > 100)
        println("EXPERIMENT_FILE=${out.absolutePath}")
        println("EXPERIMENT_COUNT=${results.length()}")
    }
}
