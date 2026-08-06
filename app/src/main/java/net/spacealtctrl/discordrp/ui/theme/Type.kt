package net.spacealtctrl.discordrp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val Face = FontFamily.SansSerif

private val Centred = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun face(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Face,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
    lineHeightStyle = Centred,
)

val AppType = Typography(
    displayLarge = face(40, 46, FontWeight.Bold, -1.0),
    displayMedium = face(34, 40, FontWeight.Bold, -0.8),
    displaySmall = face(28, 34, FontWeight.Bold, -0.5),

    headlineLarge = face(24, 30, FontWeight.Bold, -0.4),
    headlineMedium = face(21, 27, FontWeight.SemiBold, -0.3),
    headlineSmall = face(18, 24, FontWeight.SemiBold, -0.2),

    titleLarge = face(17, 23, FontWeight.SemiBold, -0.1),
    titleMedium = face(15, 21, FontWeight.SemiBold),
    titleSmall = face(14, 19, FontWeight.Medium),

    bodyLarge = face(16, 24, FontWeight.Normal),
    bodyMedium = face(15, 22, FontWeight.Normal, 0.05),
    bodySmall = face(13, 19, FontWeight.Normal, 0.1),

    labelLarge = face(14, 18, FontWeight.SemiBold, 0.1),
    labelMedium = face(12, 16, FontWeight.SemiBold, 0.4),
    labelSmall = face(11, 14, FontWeight.SemiBold, 1.0),
)
