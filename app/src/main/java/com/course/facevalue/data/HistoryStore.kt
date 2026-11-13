package com.course.facevalue.data

import android.content.Context
import com.course.facevalue.analysis.AnalysisOutcome
import com.course.facevalue.analysis.FeatureKey
import com.course.facevalue.analysis.GenderProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StoredFeature(
    val key: FeatureKey,
    val score: Float,
    val weight: Float,
    val measured: String,
    val comment: String
)

data class HistoryRecord(
    val id: String,
    val createdAt: Long,
    val profile: GenderProfile,
    val scoreRaw: Float,
    val confidence: Float,
    val grade: String,
    val thumbPath: String?,
    val features: List<StoredFeature>
) {
    val score10: Float get() = 1f + 9f * scoreRaw
}

fun AnalysisOutcome.toHistoryRecord(id: String, thumbPath: String?): HistoryRecord = HistoryRecord(
    id = id,
    createdAt = System.currentTimeMillis(),
    profile = profile,
    scoreRaw = score.raw,
    confidence = score.confidence,
    grade = score.grade,
    thumbPath = thumbPath,
    features = score.features.map {
        StoredFeature(it.key, it.score, it.weight, it.measured, it.comment)
    }
)

/** 历史记录只保存在本机 SharedPreferences 与 filesDir，不上传。 */
class HistoryStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("facevalue_history", Context.MODE_PRIVATE)
    private val maxRecords = 12

    fun load(): List<HistoryRecord> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    // 单条记录损坏时跳过该条，保留其余有效历史。
                    val record = runCatching { parse(array.getJSONObject(i)) }.getOrNull()
                    if (record != null &&
                        (record.thumbPath == null || File(record.thumbPath).exists())
                    ) add(record)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(records: List<HistoryRecord>) {
        val array = JSONArray()
        records.take(maxRecords).forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id)
                put("createdAt", r.createdAt)
                put("profile", r.profile.name)
                put("scoreRaw", r.scoreRaw.toDouble())
                put("confidence", r.confidence.toDouble())
                put("grade", r.grade)
                put("thumb", r.thumbPath ?: JSONObject.NULL)
                put("features", JSONArray().apply {
                    r.features.forEach { f ->
                        put(JSONObject().apply {
                            put("key", f.key.name)
                            put("score", f.score.toDouble())
                            put("weight", f.weight.toDouble())
                            put("measured", f.measured)
                            put("comment", f.comment)
                        })
                    }
                })
            })
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun clearThumbs(records: List<HistoryRecord>) {
        records.forEach { r -> r.thumbPath?.let { runCatching { File(it).delete() } } }
    }

    fun deleteThumb(record: HistoryRecord) {
        record.thumbPath?.let { runCatching { File(it).delete() } }
    }

    private fun parse(o: JSONObject): HistoryRecord {
        val features = mutableListOf<StoredFeature>()
        val arr = o.optJSONArray("features") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val f = arr.getJSONObject(i)
            val key = runCatching { FeatureKey.valueOf(f.getString("key")) }.getOrNull() ?: continue
            features += StoredFeature(
                key = key,
                score = f.optDouble("score", 0.0).toFloat(),
                weight = f.optDouble("weight", 0.0).toFloat(),
                measured = f.optString("measured"),
                comment = f.optString("comment")
            )
        }
        val thumb = if (o.isNull("thumb")) null else o.optString("thumb").takeIf { it.isNotBlank() }
        val profile = runCatching { GenderProfile.valueOf(o.optString("profile", "NEUTRAL")) }
            .getOrDefault(GenderProfile.NEUTRAL)
        return HistoryRecord(
            id = o.optString("id"),
            createdAt = o.optLong("createdAt"),
            profile = profile,
            scoreRaw = o.optDouble("scoreRaw", 0.0).toFloat(),
            confidence = o.optDouble("confidence", 0.0).toFloat(),
            grade = o.optString("grade"),
            thumbPath = thumb,
            features = features
        )
    }

    companion object {
        private const val KEY = "records"
        const val MAX_RECORDS = 12
    }
}
