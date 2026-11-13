package com.course.facevalue.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.course.facevalue.analysis.KeypointLayout
import com.course.facevalue.analysis.model.FaceLandmarks
import com.course.facevalue.analysis.model.Pt
import kotlin.math.min

enum class KeypointMode(val label: String) {
    OFF("关闭"),
    P68("68 点"),
    P106("106 点")
}

/**
 * 在原图上叠加人脸框、轮廓与 68/106 关键点。
 * 图片使用 ContentScale.Fit 显示，坐标按同一比例映射，保证任意屏幕尺寸下对齐。
 */
@Composable
fun FaceAnnotatedImage(
    bitmap: Bitmap,
    landmarks: FaceLandmarks,
    keypointMode: KeypointMode,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "待分析的人脸照片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()
            if (bw <= 0f || bh <= 0f) return@Canvas
            val scale = min(size.width / bw, size.height / bh)
            val dw = bw * scale
            val dh = bh * scale
            val ox = (size.width - dw) / 2f
            val oy = (size.height - dh) / 2f

            fun map(p: Pt): Offset = Offset(ox + p.x * dw, oy + p.y * dh)

            fun path(points: List<Pt>, close: Boolean): Path {
                val path = Path()
                points.forEachIndexed { i, p ->
                    val o = map(p)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                if (close) path.close()
                return path
            }

            // 轮廓线
            val outline = Color.White.copy(alpha = 0.85f)
            drawPath(path(landmarks.face, true), outline, style = Stroke(1.6.dp.toPx()))
            drawPath(path(landmarks.leftEye, true), outline, style = Stroke(1.1.dp.toPx()))
            drawPath(path(landmarks.rightEye, true), outline, style = Stroke(1.1.dp.toPx()))
            drawPath(path(landmarks.leftBrow, false), outline.copy(alpha = 0.55f), style = Stroke(1.dp.toPx()))
            drawPath(path(landmarks.rightBrow, false), outline.copy(alpha = 0.55f), style = Stroke(1.dp.toPx()))
            drawPath(path(landmarks.noseBridge + landmarks.noseBottom, false), outline.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
            drawPath(path(landmarks.upperLipTop + landmarks.lowerLipBottom, true), outline.copy(alpha = 0.7f), style = Stroke(1.dp.toPx()))

            // 人脸框四角
            val b = landmarks.bounds
            val tl = map(Pt(b.left, b.top))
            val br = map(Pt(b.right, b.bottom))
            val corner = 16.dp.toPx()
            val boxColor = Color(0xFFFF7A9E)
            val stroke = 2.dp.toPx()
            val corners = listOf(
                Triple(tl, Offset(1f, 0f), Offset(0f, 1f)),
                Triple(Offset(br.x, tl.y), Offset(-1f, 0f), Offset(0f, 1f)),
                Triple(Offset(tl.x, br.y), Offset(1f, 0f), Offset(0f, -1f)),
                Triple(br, Offset(-1f, 0f), Offset(0f, -1f))
            )
            for ((cornerPoint, dx, dy) in corners) {
                drawLine(
                    boxColor,
                    cornerPoint,
                    cornerPoint + Offset(dx.x * corner, dx.y * corner),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    boxColor,
                    cornerPoint,
                    cornerPoint + Offset(dy.x * corner, dy.y * corner),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            // 关键点
            if (keypointMode != KeypointMode.OFF) {
                val layout = if (keypointMode == KeypointMode.P68) {
                    KeypointLayout.layout68(landmarks)
                } else {
                    KeypointLayout.layout106(landmarks)
                }
                val radius = if (layout.size > 80) 1.9.dp.toPx() else 2.4.dp.toPx()
                for (item in layout) {
                    val o = map(item.point)
                    drawCircle(Color(0xFF6EF3FF), radius = radius, center = o)
                    drawCircle(Color(0xAA0B2230), radius = radius, center = o, style = Stroke(0.7.dp.toPx()))
                }
            }
        }
    }
}
