package com.unlukot.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

val Green = Color(0xFF1B8A4F)
val GreenDark = Color(0xFF0F5F36)
val GreenDeep = Color(0xFF0A4427)
val GreenMid = Color(0xFF3FB36F)
val GreenSoft = Color(0xFFCDEBD8)
val GreenLight = Color(0xFFE7F6EC)
val Mint = Color(0xFF7FE0A8)
val Ink = Color(0xFF12261A)
val InkSoft = Color(0xFF4A6355)

private val LightScheme = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = GreenMid,
    onSecondary = Color.White,
    secondaryContainer = GreenSoft,
    onSecondaryContainer = GreenDark,
    tertiary = Color(0xFFE08A00),
    background = Color(0xFFF6FAF7),
    onBackground = Ink,
    surface = Color(0xFFF6FAF7),
    onSurface = Ink,
    surfaceVariant = GreenLight,
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFF6F1),
    surfaceContainerHigh = Color(0xFFE7F1EA),
    surfaceContainerHighest = Color(0xFFDDEBE2),
    outline = Color(0xFFB9D3C3),
    outlineVariant = Color(0xFFDDEBE2),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF8C1D1D)
)

private val DarkScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF16432B),
    onPrimaryContainer = Color(0xFFB8F2CC),
    secondary = GreenMid,
    onSecondary = Color(0xFF00210E),
    secondaryContainer = Color(0xFF1F3A2A),
    onSecondaryContainer = Color(0xFFC6E9D3),
    tertiary = Color(0xFFFFB951),
    background = Color(0xFF0D1511),
    onBackground = Color(0xFFDDE7E0),
    surface = Color(0xFF0D1511),
    onSurface = Color(0xFFDDE7E0),
    surfaceVariant = Color(0xFF1B2721),
    onSurfaceVariant = Color(0xFFA6BBAE),
    surfaceContainerLowest = Color(0xFF09100C),
    surfaceContainerLow = Color(0xFF141E18),
    surfaceContainer = Color(0xFF18231D),
    surfaceContainerHigh = Color(0xFF1F2B24),
    surfaceContainerHighest = Color(0xFF26332B),
    outline = Color(0xFF3E5246),
    outlineVariant = Color(0xFF26332B),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0A0A),
    errorContainer = Color(0xFF4A1515),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** UnLukot's green & white brand, with a matching dark mode. */
@Composable
fun UnLukotTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
