package com.course.facevalue.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.facevalue.analysis.FeatureKey
import com.course.facevalue.analysis.FeatureScore
import com.course.facevalue.analysis.ScoreScale
import com.course.facevalue.ui.theme.Hairline
import com.course.facevalue.ui.theme.Ink
import com.course.facevalue.ui.theme.InkMuted
import com.course.facevalue.ui.theme.Rose
import com.course.facevalue.ui.theme.Violet
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ScoreRing(
    raw: Float,
    scale: ScoreScale,
    grade: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = 148.dp
) {
    val animated by animateFloatAsState(
        targetValue = raw.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "score"
    )
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2f
            drawArc(
                color = Color(0xFFF0EBF5),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.linearGradient(
                    listOf(Violet, Rose),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    scale.format(raw),
                    style = MaterialTheme.typography.displayLarge,
                    color = Ink,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "/${scale.max}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                )
            }
            Text(
                grade.substringBefore(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = Violet,
                fontWeight = FontWeight.SemiBold
            )
            Text("几何协调度", style = MaterialTheme.typography.labelSmall, color = InkMuted)
        }
    }
}

@Composable
fun RadarChart(features: List<FeatureScore>, modifier: Modifier = Modifier) {
    fun score(key: FeatureKey): Float = features.firstOrNull { it.key == key }?.score ?: 0.6f
    val items = listOf(
        "对称" to score(FeatureKey.SYMMETRY),
        "比例" to (score(FeatureKey.THIRDS) + score(FeatureKey.FIVE_EYES)) / 2f,
        "眉眼" to (score(FeatureKey.BROW_EYE) + score(FeatureKey.EYE_SHAPE)) / 2f,
        "鼻唇" to (score(FeatureKey.NOSE) + score(FeatureKey.LIPS)) / 2f,
        "轮廓" to score(FeatureKey.CONTOUR),
        "肤质" to (score(FeatureKey.SKIN) + score(FeatureKey.EXPRESSION)) / 2f
    )
    val animated by animateFloatAsState(1f, animationSpec = tween(700), label = "radar")

    Canvas(modifier = modifier.fillMaxSize()) {
        val labelSpace = 26.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = (minOf(size.width, size.height) / 2f - labelSpace).coerceAtLeast(20.dp.toPx())
        val count = items.size
        val n = items.size

        fun point(index: Int, factor: Float): Offset {
            val angle = (-90f + 360f * index / n) * Math.PI.toFloat() / 180f
            return Offset(cx + cos(angle) * radius * factor, cy + sin(angle) * radius * factor)
        }

        // 同心六边形
        for (level in 1..3) {
            val path = Path()
            for (i in 0 until count) {
                val p = point(i, level / 3f)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, color = Color(0xFFEAE4F3), style = Stroke(1.dp.toPx()))
        }
        for (i in 0 until count) {
            drawLine(Color(0xFFEAE4F3), Offset(cx, cy), point(i, 1f), strokeWidth = 1.dp.toPx())
        }

        val dataPath = Path()
        items.forEachIndexed { i, item ->
            val p = point(i, (0.15f + 0.85f * item.second.coerceIn(0f, 1f)) * animated)
            if (i == 0) dataPath.moveTo(p.x, p.y) else dataPath.lineTo(p.x, p.y)
        }
        dataPath.close()
        drawPath(dataPath, brush = Brush.radialGradient(listOf(Violet.copy(alpha = 0.45f), Rose.copy(alpha = 0.18f)), center = Offset(cx, cy), radius = radius))
        drawPath(dataPath, color = Violet, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        items.forEachIndexed { i, item ->
            val p = point(i, (0.15f + 0.85f * item.second.coerceIn(0f, 1f)) * animated)
            drawCircle(Color.White, radius = 3.2.dp.toPx(), center = p)
            drawCircle(Violet, radius = 2.2.dp.toPx(), center = p)
        }

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#6F6A80")
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }
        items.forEachIndexed { i, item ->
            val p = point(i, 1.22f)
            drawContext.canvas.nativeCanvas.drawText(item.first, p.x, p.y + paint.textSize / 3f, paint)
        }
    }
}

@Composable
fun FeatureScoreRow(feature: FeatureScore, scale: ScoreScale) {
    val animated by animateFloatAsState(
        targetValue = feature.score.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "feature"
    )
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                feature.key.label,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF4F0FA))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "权重 ${(feature.weight * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                String.format(Locale.US, "%.1f", scale.display(feature.score)),
                style = MaterialTheme.typography.titleMedium,
                color = if (feature.score >= 0.7f) Violet else InkMuted,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFF2EDF7))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Violet, Rose)))
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(feature.measured, style = MaterialTheme.typography.labelSmall, color = Violet)
        Spacer(Modifier.height(2.dp))
        Text(feature.comment, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    }
}
