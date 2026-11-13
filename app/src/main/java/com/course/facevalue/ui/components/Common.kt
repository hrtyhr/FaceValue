package com.course.facevalue.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.facevalue.ui.theme.Hairline
import com.course.facevalue.ui.theme.Ink
import com.course.facevalue.ui.theme.InkMuted
import com.course.facevalue.ui.theme.Rose
import com.course.facevalue.ui.theme.Violet
import com.course.facevalue.ui.theme.VioletSoft

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SelectPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Brush.horizontalGradient(listOf(Violet, Rose)) else Brush.horizontalGradient(
        listOf(Color.White, Color.White)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else Hairline,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else InkMuted
        )
    }
}

@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .shadow(14.dp, RoundedCornerShape(50), ambientColor = Violet.copy(alpha = 0.35f), spotColor = Violet.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Violet, Rose)))
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (icon == null) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Hairline),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        Box(modifier = Modifier.padding(padding.dp)) { content() }
    }
}

@Composable
fun EthicsStrip(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VioletSoft)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Shield, contentDescription = null, tint = Violet, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "本地处理 · 不上传人脸",
                style = MaterialTheme.typography.labelLarge,
                color = Violet,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "分数是人群主观偏好的统计近似，不代表客观美丑或人的价值。",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
                lineHeight = 15.sp
            )
        }
    }
}
