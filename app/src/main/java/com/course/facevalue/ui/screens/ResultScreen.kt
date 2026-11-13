package com.course.facevalue.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.facevalue.analysis.AnalysisOutcome
import com.course.facevalue.analysis.FeatureScore
import com.course.facevalue.analysis.GenderProfile
import com.course.facevalue.analysis.ImageSource
import com.course.facevalue.analysis.ScoreScale
import com.course.facevalue.analysis.ScoreResult
import com.course.facevalue.data.HistoryRecord
import com.course.facevalue.ui.AppViewModel
import com.course.facevalue.ui.components.FaceAnnotatedImage
import com.course.facevalue.ui.components.FeatureScoreRow
import com.course.facevalue.ui.components.KeypointMode
import com.course.facevalue.ui.components.RadarChart
import com.course.facevalue.ui.components.ScoreRing
import com.course.facevalue.ui.components.SectionHeader
import com.course.facevalue.ui.components.SelectPill
import com.course.facevalue.ui.components.SoftCard
import com.course.facevalue.ui.theme.Cream
import com.course.facevalue.ui.theme.Hairline
import com.course.facevalue.ui.theme.Ink
import com.course.facevalue.ui.theme.InkMuted
import com.course.facevalue.ui.theme.Mint
import com.course.facevalue.ui.theme.Rose
import com.course.facevalue.ui.theme.Violet
import com.course.facevalue.ui.theme.VioletSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreen(vm: AppViewModel, outcome: AnalysisOutcome) {
    val context = LocalContext.current
    var keypointMode by remember { mutableStateOf(KeypointMode.P106) }
    var infoVisible by remember { mutableStateOf(false) }
    var shareVisible by remember { mutableStateOf(false) }
    val scale = vm.scale

    if (infoVisible) {
        ScoreInfoDialog(onDismiss = { infoVisible = false })
    }
    if (shareVisible) {
        ScoreInfoDialog(
            onDismiss = { shareVisible = false },
            title = "分享评分摘要",
            confirmText = "发送",
            customText = buildShareText(outcome, scale),
            onConfirm = {
                shareVisible = false
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, buildShareText(outcome, scale))
                }
                context.startActivity(Intent.createChooser(intent, "分享评分摘要"))
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Cream),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TopBar(
                title = "分析结果",
                modifier = Modifier.statusBarsPadding(),
                onBack = { vm.goHome() },
                actions = {
                    IconButton(onClick = { shareVisible = true }) {
                        Icon(Icons.Rounded.Share, contentDescription = "分享", tint = Ink)
                    }
                    IconButton(onClick = { infoVisible = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "评分说明", tint = Ink)
                    }
                }
            )
        }

        item {
            AnnotatedImageCard(
                outcome = outcome,
                keypointMode = keypointMode,
                onModeChange = { keypointMode = it }
            )
        }

        item { ScoreSummaryCard(outcome = outcome, scale = scale, onOpenInfo = { infoVisible = true }) }

        item {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "分性别打分依据",
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(VioletSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${outcome.profile.label}预设",
                                style = MaterialTheme.typography.labelSmall,
                                color = Violet
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "切换权重会即时重新加权，无需重新检测",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GenderProfile.entries.forEach { profile ->
                            SelectPill(
                                text = profile.label,
                                selected = outcome.profile == profile,
                                onClick = { vm.selectProfile(profile) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(outcome.profile.note, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                    Spacer(Modifier.height(8.dp))
                    ProfileBasisText(outcome.profile)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "说明：以上权重参考 SCUT-FBP5500、MEBeauty 等公开数据集对群体审美评分的统计结论，映射为可解释的几何指标权重；本 App 未用这些数据训练模型。权重反映的是数据集中的人群偏好，不代表客观美丑，也不能推广到每一个个体。",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item { FeatureBreakdownCard(features = outcome.score.features, scale = scale) }

        item {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = Rose, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("提升测量稳定性的建议", style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• 正对镜头，头部左右偏转不超过 15°；\n• 光线均匀，避免一侧强光或油光；\n• 露出额头与眉毛，避免刘海遮挡；\n• 放松表情，不要刻意眯眼或咬唇。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ButtonChip("再测一张", Icons.Rounded.Refresh, vm::goHome)
                        ButtonChip("伦理说明", Icons.Rounded.Info, vm::openEthics)
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                Text(
                    "本结果仅用于课程设计与技术演示，不是医学、心理或身份判断工具。",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "分析用时 ${outcome.elapsedMs} ms · ${outcome.imageWidth}×${outcome.imageHeight} px · 面部轮廓点 ${outcome.landmarks.contourCount()} 个",
                    style = MaterialTheme.typography.labelSmall,
                    color = Violet.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StoredDetailScreen(vm: AppViewModel, record: HistoryRecord) {
    val scale = vm.scale
    val storedScore = remember(record) {
        ScoreResult(
            raw = record.scoreRaw,
            confidence = record.confidence,
            features = record.features.map {
                FeatureScore(it.key, it.score, it.weight, it.measured, it.comment)
            },
            profile = record.profile,
            highlights = emptyList(),
            suggestions = emptyList()
        )
    }
    val bitmap = remember(record.thumbPath) {
        record.thumbPath?.let { runCatching { android.graphics.BitmapFactory.decodeFile(it) }.getOrNull() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Cream),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TopBar(
                title = "历史记录",
                modifier = Modifier.statusBarsPadding(),
                onBack = { vm.goHome() },
                actions = {
                    IconButton(onClick = { vm.deleteHistory(record) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = Ink)
                    }
                }
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFF0EAF7))
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("保存于历史记录", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(record.createdAt)),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        item { ScoreSummaryCard(score = storedScore, scale = scale, onOpenInfo = { vm.openEthics() }, showMeta = false) }
        item {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("分性别打分依据", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(storedScore.profile.note, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                    Spacer(Modifier.height(6.dp))
                    ProfileBasisText(storedScore.profile)
                }
            }
        }
        item { FeatureBreakdownCard(features = storedScore.features, scale = scale) }
        item {
            Text(
                "历史记录只保存在本机，缩略图用于回顾；删除记录会同时删除缩略图。",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = Ink)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
private fun AnnotatedImageCard(
    outcome: AnalysisOutcome,
    keypointMode: KeypointMode,
    onModeChange: (KeypointMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF17131F))
    ) {
        FaceAnnotatedImage(
            bitmap = outcome.bitmap,
            landmarks = outcome.landmarks,
            keypointMode = keypointMode,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(50))
                .padding(3.dp)
        ) {
            KeypointMode.entries.forEach { mode ->
                val selected = keypointMode == mode
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) Color.White else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        mode.label,
                        color = if (selected) Ink else Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    when (outcome.source) {
                        ImageSource.GALLERY -> "相册图片"
                        ImageSource.CAMERA -> "相机拍摄"
                        ImageSource.SAMPLE -> "合成人脸示例"
                        ImageSource.LIVE -> "实时检测"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${outcome.imageWidth} × ${outcome.imageHeight} px · 检测到 ${outcome.landmarks.contourCount()} 个轮廓点",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ScoreSummaryCard(
    outcome: AnalysisOutcome,
    scale: ScoreScale,
    onOpenInfo: () -> Unit
) {
    ScoreSummaryCard(outcome.score, scale, onOpenInfo, showMeta = true, elapsedMs = outcome.elapsedMs)
}

@Composable
private fun ScoreSummaryCard(
    score: ScoreResult,
    scale: ScoreScale,
    onOpenInfo: () -> Unit,
    showMeta: Boolean,
    elapsedMs: Long = 0
) {
    SoftCard(modifier = Modifier.fillMaxWidth(), padding = 18) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(
                    raw = score.raw,
                    scale = scale,
                    grade = score.grade,
                    modifier = Modifier.padding(4.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        score.grade.substringAfter(" · ", score.grade),
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "按「${score.profile.label}」权重综合",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetaChip("可信度 ${score.confidenceLabel}", if (score.confidence >= 0.6f) Mint else Rose)
                        MetaChip("${score.profile.label}权重", Violet)
                    }
                    if (showMeta) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "分析用时 $elapsedMs ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                RadarChart(features = score.features, modifier = Modifier.fillMaxSize())
            }
            Text(
                "优势：${score.highlights.joinToString("、").ifBlank { "暂无明显优势项" }}　·　可改善：${score.suggestions.joinToString("、").ifBlank { "—" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "点击查看评分含义",
                style = MaterialTheme.typography.labelSmall,
                color = Violet,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenInfo),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureBreakdownCard(features: List<FeatureScore>, scale: ScoreScale) {
    Column {
        SectionHeader("评分依据", "共 ${features.size} 项指标，按权重排序；数值为 ${scale.label}")
        Spacer(Modifier.height(10.dp))
        SoftCard(modifier = Modifier.fillMaxWidth(), padding = 18) {
            Column {
                features.forEachIndexed { index, feature ->
                    FeatureScoreRow(feature = feature, scale = scale)
                    if (index != features.lastIndex) {
                        Divider(color = Hairline, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBasisText(profile: GenderProfile) {
    val text = when (profile) {
        GenderProfile.MALE ->
            "男性预设：轮廓与下颌线 20%、对称性 18%、五眼比例 12%、眉眼距 10%、鼻部比例 10%，唇形仅 6%。统计研究常把男性面部吸引力与下颌轮廓清晰度、眉眼间距和面部横向比例联系在一起。"
        GenderProfile.FEMALE ->
            "女性预设：对称性 18%、唇形 14%、三庭与五眼各 12%、眼型 12%，轮廓保留 10%。统计研究常把女性面部吸引力与唇部厚薄比例、眼裂形态、面部柔和度联系在一起。"
        GenderProfile.NEUTRAL ->
            "通用预设：对称性 18%、轮廓 15%、三庭 11%、五眼 12%，其余指标均衡分配，不对任一性别偏好加权。"
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
}

@Composable
private fun MetaChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ButtonChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(VioletSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = Violet)
    }
}

@Composable
private fun ScoreInfoDialog(
    onDismiss: () -> Unit,
    title: String = "评分是怎么算出来的？",
    confirmText: String = "我知道了",
    customText: String? = null,
    onConfirm: () -> Unit = onDismiss
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Ink) },
        text = {
            Text(
                customText ?: "1. 在图像中检测人脸并提取 13 组轮廓（130+ 点），重采样为 68/106 点布局；\n" +
                    "2. 计算三庭、五眼、对称性、眉眼距、鼻部、唇形、下颌轮廓、眼型等几何比例，并从脸颊/额头采样肤色均匀度；\n" +
                    "3. 每项指标用高斯隶属函数映射为 0–1 的子分数，再按当前偏好权重加权，线性映射到 1–10 或 1–5 分；\n" +
                    "4. 可信度由人脸尺寸、头部姿态与轮廓完整度估计。分数只是统计偏好的近似，不是客观美丑或人的价值。",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = Violet) }
        },
        dismissButton = if (customText != null) {
            { TextButton(onClick = onDismiss) { Text("取消", color = InkMuted) } }
        } else null
    )
}

private fun buildShareText(outcome: AnalysisOutcome, scale: ScoreScale): String {
    val thirds = outcome.measurements.thirds.joinToString(" : ") { String.format(Locale.US, "%.2f", it) }
    return buildString {
        appendLine("我在「颜析 FaceValue」完成了一次面部几何分析：")
        appendLine("综合评分 ${scale.format(outcome.score.raw)}/${scale.max}（${outcome.profile.label}权重 · 可信度 ${outcome.score.confidenceLabel}）")
        appendLine("三庭比例 $thirds")
        appendLine("对称性偏差 ${String.format(Locale.US, "%.1f", outcome.measurements.symmetryRms * 100)}% 面宽")
        append("分数来自人群主观评分的统计近似，不代表客观美丑；人脸图像仅在本机处理。")
    }
}
