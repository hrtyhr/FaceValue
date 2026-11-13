package com.course.facevalue.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.course.facevalue.analysis.AnalysisOutcome
import com.course.facevalue.analysis.FaceAnalysisEngine
import com.course.facevalue.analysis.GenderProfile
import com.course.facevalue.analysis.ImageSource
import com.course.facevalue.analysis.ScoreScale
import com.course.facevalue.analysis.ScoringEngine
import com.course.facevalue.data.HistoryRecord
import com.course.facevalue.data.HistoryStore
import com.course.facevalue.data.StoredFeature
import com.course.facevalue.data.toHistoryRecord
import com.course.facevalue.util.BitmapUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

sealed interface Screen {
    data object Home : Screen
    data class Camera(val live: Boolean = false) : Screen
    data object Ethics : Screen
    data class Result(val outcome: AnalysisOutcome) : Screen
    data class StoredDetail(val record: HistoryRecord) : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    var profile by mutableStateOf(loadProfile())
        private set

    var scale by mutableStateOf(loadScale())
        private set

    var analyzing by mutableStateOf(false)
        private set

    var analyzingLabel by mutableStateOf("正在分析…")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var history by mutableStateOf<List<HistoryRecord>>(emptyList())
        private set

    private val engine = FaceAnalysisEngine(accurate = true)
    private val historyStore = HistoryStore(app)
    private val historyMutex = Mutex()
    private val settings = app.getSharedPreferences("facevalue_settings", Application.MODE_PRIVATE)

    init {
        history = historyStore.load()
    }

    private fun loadProfile(): GenderProfile = runCatching {
        GenderProfile.valueOf(
            getApplication<Application>()
                .getSharedPreferences("facevalue_settings", Application.MODE_PRIVATE)
                .getString("profile", GenderProfile.NEUTRAL.name) ?: GenderProfile.NEUTRAL.name
        )
    }.getOrDefault(GenderProfile.NEUTRAL)

    private fun loadScale(): ScoreScale = runCatching {
        ScoreScale.valueOf(
            getApplication<Application>()
                .getSharedPreferences("facevalue_settings", Application.MODE_PRIVATE)
                .getString("scale", ScoreScale.TEN.name) ?: ScoreScale.TEN.name
        )
    }.getOrDefault(ScoreScale.TEN)

    fun selectProfile(value: GenderProfile) {
        profile = value
        settings.edit().putString("profile", value.name).apply()
        val current = screen
        if (current is Screen.Result) {
            val outcome = current.outcome
            val rescored = outcome.copy(
                score = ScoringEngine.score(outcome.measurements, outcome.skin, value),
                profile = value
            )
            screen = Screen.Result(rescored)
            updateHistoryProfile(rescored)
        }
    }

    /** 结果页切换权重后，同步更新已保存的历史记录，避免历史卡片与当前结果不一致。 */
    private fun updateHistoryProfile(outcome: AnalysisOutcome) {
        if (outcome.analysisId.isEmpty()) return
        val index = history.indexOfFirst { it.id == outcome.analysisId }
        if (index < 0) return
        val updated = history.toMutableList()
        updated[index] = updated[index].copy(
            profile = outcome.profile,
            scoreRaw = outcome.score.raw,
            confidence = outcome.score.confidence,
            grade = outcome.score.grade,
            features = outcome.score.features.map {
                StoredFeature(it.key, it.score, it.weight, it.measured, it.comment)
            }
        )
        history = updated
        viewModelScope.launch { saveHistory() }
    }

    private suspend fun saveHistory() {
        historyMutex.withLock {
            val snapshot = history
            withContext(Dispatchers.IO) { historyStore.save(snapshot) }
        }
    }

    fun selectScale(value: ScoreScale) {
        scale = value
        settings.edit().putString("scale", value.name).apply()
    }

    fun openCamera(live: Boolean = false) {
        error = null
        screen = Screen.Camera(live)
    }

    fun openEthics() {
        screen = Screen.Ethics
    }

    fun goHome() {
        screen = Screen.Home
    }

    fun openHistory(record: HistoryRecord) {
        screen = Screen.StoredDetail(record)
    }

    fun dismissError() {
        error = null
    }

    fun analyzeUri(uri: Uri) = startAnalyze(ImageSource.GALLERY, "正在读取相册图片…") {
        withContext(Dispatchers.IO) { BitmapUtils.loadFromUri(getApplication(), uri) }
    }

    fun analyzePhoto(file: File) {
        if (analyzing) {
            runCatching { file.delete() }
            return
        }
        startAnalyze(ImageSource.CAMERA, "正在读取照片…") {
            withContext(Dispatchers.IO) {
                val bitmap = BitmapUtils.loadFromFile(file.absolutePath)
                runCatching { file.delete() }
                bitmap
            }
        }
    }

    fun analyzeSample(assetName: String) = startAnalyze(ImageSource.SAMPLE, "正在加载示例图片…") {
        withContext(Dispatchers.IO) {
            BitmapUtils.loadFromStream({ getApplication<Application>().assets.open(assetName) })
        }
    }

    private fun startAnalyze(source: ImageSource, label: String, loader: suspend () -> Bitmap) {
        if (analyzing) return
        val origin = screen
        viewModelScope.launch {
            analyzing = true
            analyzingLabel = label
            error = null
            try {
                val bitmap = loader()
                analyzingLabel = "正在检测人脸与关键点…"
                val outcome = engine.analyze(bitmap, profile, source)
                    .copy(analysisId = newHistoryId())
                // 用户在分析期间离开当前页面时，不再用旧结果覆盖新页面。
                if (screen === origin) {
                    try {
                        persist(outcome)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // 历史保存失败不应影响结果展示。
                    }
                    if (screen === origin) screen = Screen.Result(outcome)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message?.takeIf { it.isNotBlank() } ?: "分析失败，请重试。"
                error = if (e is com.course.facevalue.analysis.NoFaceException ||
                    e is com.course.facevalue.analysis.LandmarkException
                ) {
                    message
                } else {
                    "分析失败：$message"
                }
            } finally {
                analyzing = false
            }
        }
    }

    private fun newHistoryId(): String =
        "fv_" + System.currentTimeMillis().toString(36) + "_" + Random.nextInt(1000)

    private suspend fun persist(outcome: AnalysisOutcome) {
        val id = outcome.analysisId
        val record = withContext(Dispatchers.IO) {
            val thumb = BitmapUtils.saveThumbnail(getApplication(), outcome.bitmap, id)
            outcome.toHistoryRecord(id, thumb?.absolutePath)
        }
        historyMutex.withLock {
            val merged = listOf(record) + history
            val kept = merged.take(HistoryStore.MAX_RECORDS)
            val dropped = merged.drop(HistoryStore.MAX_RECORDS)
            withContext(Dispatchers.IO) {
                dropped.forEach { historyStore.deleteThumb(it) }
                historyStore.save(kept)
            }
            history = kept
        }
    }

    fun deleteHistory(record: HistoryRecord) {
        val updated = history.filterNot { it.id == record.id }
        viewModelScope.launch(Dispatchers.IO) { historyStore.deleteThumb(record) }
        history = updated
        viewModelScope.launch { saveHistory() }
        if ((screen as? Screen.StoredDetail)?.record?.id == record.id) screen = Screen.Home
    }

    fun clearHistory() {
        val snapshot = history
        viewModelScope.launch(Dispatchers.IO) { historyStore.clearThumbs(snapshot) }
        history = emptyList()
        viewModelScope.launch { saveHistory() }
    }

    override fun onCleared() {
        super.onCleared()
        engine.close()
    }
}
