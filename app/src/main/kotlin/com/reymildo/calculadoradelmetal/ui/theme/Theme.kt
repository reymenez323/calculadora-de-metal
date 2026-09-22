package com.reymildo.calculadoradelmetal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFFC2571A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBEEE4),
    onPrimaryContainer = Color(0xFF5C2508),
    secondary = Color(0xFF6B6F72),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF2F0EC),
    onBackground = Color(0xFF17191A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17191A),
    surfaceVariant = Color(0xFFFAF8F5),
    onSurfaceVariant = Color(0xFF6B6F72),
    surfaceContainerHighest = Color(0xFFFAF8F5),
    outline = Color(0xFFBDB8B0),
    outlineVariant = Color(0xFFE2DED7),
    error = Color(0xFFB3261E),
    inverseSurface = Color(0xFF17191A),
    inverseOnSurface = Color(0xFFF2F0EC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF08A3C),
    onPrimary = Color(0xFF3A1B05),
    primaryContainer = Color(0xFF3A2718),
    onPrimaryContainer = Color(0xFFFFD9BE),
    secondary = Color(0xFF9AA09F),
    onSecondary = Color(0xFF1B1E1E),
    background = Color(0xFF121413),
    onBackground = Color(0xFFECEEEC),
    surface = Color(0xFF1B1E1E),
    onSurface = Color(0xFFECEEEC),
    surfaceVariant = Color(0xFF232626),
    onSurfaceVariant = Color(0xFF9AA09F),
    surfaceContainerHighest = Color(0xFF232626),
    outline = Color(0xFF5B605F),
    outlineVariant = Color(0xFF323635),
    error = Color(0xFFF2B8B5),
    inverseSurface = Color(0xFF26292A),
    inverseOnSurface = Color(0xFFECEEEC),
)

/**
 * Familia para cifras. Si quieres el look del prototipo (IBM Plex Mono / Manrope),
 * mete los .ttf en res/font y cambia estas dos constantes por FontFamily(Font(R.font.…)).
 */
val NumberFamily: FontFamily = FontFamily.Monospace
private val UiFamily: FontFamily = FontFamily.Default

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = NumberFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, lineHeight = 18.sp),
    bodyMedium = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = UiFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 1.sp),
)

@Composable
fun CalculadoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
