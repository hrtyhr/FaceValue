package com.course.facevalue.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.facevalue.analysis.GenderProfile
import com.course.facevalue.analysis.ScoreScale
import com.course.facevalue.data.HistoryRecord
import com.course.facevalue.ui.AppViewModel
import com.course.facevalue.ui.components.EthicsStrip
import com.course.facevalue.ui.components.SectionHeader
import com.course.facevalue.ui.components.SelectPill
import com.course.facevalue.ui.components.SoftCard
import com.course.facevalue.ui.theme.Cream
import com.course.facevalue.ui.theme.Hairline
import com.course.facevalue.ui.theme.Ink
import com.course.facevalue.ui.theme.InkMuted
import com.course.facevalue.ui.theme.Rose
import com.course.facevalue.ui.theme.Violet
import com.course.facevalue.ui.theme.VioletSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: AppViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyzeUri(uri)
    }

    Box(modifier = Modifier.fillMaxSize().background(Cream)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Brush.verticalGradient(listOf(VioletSoft.copy(alpha = 0.9f), Cream)))
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Violet, Rose))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("颜", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("颜析 FaceValue", style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text(
                            "面部几何特征分析 · 课程设计",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                    }
                    IconButton(onClick = { vm.openEthics() }) {
                        Icon(Icons.Rounded.Shield, contentDescription = "伦理说明", tint = Violet)
                    }
                }
            }

            item { HeroCard() }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = Violet) },
                        title = "相册选图",
                        subtitle = "从本地照片分析",
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Rose) },
                        title = "拍照分析",
                        subtitle = "调用摄像头拍摄",
                        onClick = { vm.openCamera(live = false) }
                    )
                }
            }

            item {
                SoftCard(modifier = Modifier.fillMaxWidth(), padding = 16) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.openCamera(live = true) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(VioletSoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Visibility, contentDescription = null, tint = Violet)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("实时人脸检测", style = MaterialTheme.typography.titleMedium, color = Ink)
                            Text(
                                "前置摄像头 · 关键点叠加 · 约 15 fps",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = InkMuted)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Rose,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "没有合适的照片？试试合成人脸示例（非真实人物）",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "示例 1",
                        style = MaterialTheme.typography.labelLarge,
                        color = Violet,
                        modifier = Modifier
                            .clickable { vm.analyzeSample("sample_1.jpg") }
                            .padding(6.dp)
                    )
                    Text(
                        "示例 2",
                        style = MaterialTheme.typography.labelLarge,
                        color = Violet,
                        modifier = Modifier
                            .clickable { vm.analyzeSample("sample_2.jpg") }
                            .padding(6.dp)
                    )
                }
            }

            item {
                Column {
                    SectionHeader(
                        title = "评分偏好",
                        subtitle = "仅调整指标权重，不做性别识别"
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GenderProfile.entries.forEach { profile ->
                            SelectPill(
                                text = profile.label,
                                selected = vm.profile == profile,
                                onClick = { vm.selectProfile(profile) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        vm.profile.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("计分制", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(
                            "同一结果可换算 1–5 或 1–10 分",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                    }
                    ScoreScale.entries.forEach { scale ->
                        SelectPill(
                            text = if (scale == ScoreScale.TEN) "10 分制" else "5 分制",
                            selected = vm.scale == scale,
                            onClick = { vm.selectScale(scale) },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "最近分析",
                    subtitle = if (vm.history.isEmpty()) "结果与缩略图只保存在本机" else "共 ${vm.history.size} 条 · 仅本机保存",
                    trailing = if (vm.history.isNotEmpty()) {
                        {
                            IconButton(onClick = { vm.clearHistory() }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "清空历史",
                                    tint = InkMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else null
                )
            }

            item {
                if (vm.history.isEmpty()) {
                    SoftCard(modifier = Modifier.fillMaxWidth(), padding = 18) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = InkMuted)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "还没有分析记录。选择一张照片，几秒后就能看到 10 项几何指标与评分依据。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(vm.history, key = { it.id }) { record ->
                            HistoryCard(
                                record = record,
                                scale = vm.scale,
                                onClick = { vm.openHistory(record) }
                            )
                        }
                    }
                }
            }

            item {
                EthicsStrip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { vm.openEthics() }
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "分数来自人群主观评分的统计近似，不代表客观美丑，也不构成任何诊断。",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "人脸图像全程本地处理 · 无网络权限 · 可随时清空记录",
                        style = MaterialTheme.typography.labelSmall,
                        color = Violet.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(222.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF5B3DF5), Color(0xFF9A5BF0), Color(0xFFF06A8E))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                Color.White.copy(alpha = 0.08f),
                radius = size.height * 0.55f,
                center = Offset(size.width * 0.92f, size.height * 0.1f)
            )
            drawCircle(
                Color.White.copy(alpha = 0.06f),
                radius = size.height * 0.38f,
                center = Offset(size.width * 0.86f, size.height * 0.76f)
            )
            drawCircle(
                Color.White.copy(alpha = 0.1f),
                radius = size.height * 0.06f,
                center = Offset(size.width * 0.78f, size.height * 0.3f)
            )
        }
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("106 点 · 10 项指标", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("本地推理", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "用几何\n读懂一张脸",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                lineHeight = 36.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "三庭五眼 · 对称性 · 眉眼鼻唇 · 轮廓比例\n给出可解释的颜值评分与分性别依据",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
        Icon(
            Icons.Rounded.Face,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 8.dp)
                .size(96.dp)
        )
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Hairline),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFF6F1FF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = InkMuted)
        }
    }
}

@Composable
private fun HistoryCard(record: HistoryRecord, scale: ScoreScale, onClick: () -> Unit) {
    val bitmap = remember(record.thumbPath) {
        record.thumbPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF0EAF7))
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Rounded.Face,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.align(Alignment.Center).size(36.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    scale.format(record.scoreRaw),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            record.profile.label + "权重",
            style = MaterialTheme.typography.labelSmall,
            color = Ink,
            fontWeight = FontWeight.Medium
        )
        Text(
            SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(record.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted
        )
    }
}
