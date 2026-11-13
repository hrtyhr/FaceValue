package com.course.facevalue.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletSoft,
    onPrimaryContainer = VioletDeep,
    secondary = Rose,
    onSecondary = Color.White,
    secondaryContainer = RoseSoft,
    onSecondaryContainer = Color(0xFF5C1B2C),
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF4F0F8),
    onSurfaceVariant = InkMuted,
    outline = Hairline,
    error = Color(0xFFC93C4E)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 52.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, lineHeight = 15.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FaceValueTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = AppTypography, shapes = AppShapes, content = content)
}
