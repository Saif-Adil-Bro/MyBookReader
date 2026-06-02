package com.myreader.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.myreader.app.R

// ─── Font Families ────────────────────────────────────────────────────────
val NunitoFamily = FontFamily(
    Font(R.font.nunito_light,     FontWeight.Light),
    Font(R.font.nunito_regular,   FontWeight.Normal),
    Font(R.font.nunito_medium,    FontWeight.Medium),
    Font(R.font.nunito_semibold,  FontWeight.SemiBold),
    Font(R.font.nunito_bold,      FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

val PlayfairFamily = FontFamily(
    Font(R.font.playfair_regular, FontWeight.Normal),
    Font(R.font.playfair_medium,  FontWeight.Medium),
    Font(R.font.playfair_bold,    FontWeight.Bold),
)

// ─── Typography ───────────────────────────────────────────────────────────
val MyReaderTypography = Typography(
    displayLarge  = TextStyle(fontFamily = PlayfairFamily, fontWeight = FontWeight.Bold,   fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = PlayfairFamily, fontWeight = FontWeight.Bold,   fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = PlayfairFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = PlayfairFamily, fontWeight = FontWeight.Bold,   fontSize = 32.sp),
    headlineMedium= TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.Bold,   fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.SemiBold,fontSize = 24.sp),
    titleLarge    = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.Bold,   fontSize = 22.sp),
    titleMedium   = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.SemiBold,fontSize = 16.sp, letterSpacing = 0.15.sp),
    titleSmall    = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium    = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall     = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge    = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.SemiBold,fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = NunitoFamily,   fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

// ─── Color Schemes ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Emerald600,
    onPrimary        = White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary        = Gold500,
    onSecondary      = White,
    secondaryContainer = Gold50,
    onSecondaryContainer = Gold700,
    tertiary         = InfoBlue,
    onTertiary       = White,
    background       = BackgroundLight,
    onBackground     = Color(0xFF1A1A2E),
    surface          = SurfaceLight,
    onSurface        = Color(0xFF1A1A2E),
    surfaceVariant   = NeutralLight,
    onSurfaceVariant = NeutralDark,
    outline          = NeutralMid,
    error            = ErrorRed,
    onError          = White,
)

private val DarkColorScheme = darkColorScheme(
    primary          = Emerald400,
    onPrimary        = Emerald950,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald100,
    secondary        = Gold400,
    onSecondary      = Color(0xFF1A1A2E),
    secondaryContainer = Gold700,
    onSecondaryContainer = Gold200,
    tertiary         = InfoBlue,
    onTertiary       = White,
    background       = BackgroundDark,
    onBackground     = Color(0xFFE2E8F0),
    surface          = SurfaceDark,
    onSurface        = Color(0xFFE2E8F0),
    surfaceVariant   = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline          = Color(0xFF475569),
    error            = Color(0xFFFCA5A5),
    onError          = Color(0xFF7F1D1D),
)

// ─── Reader Theme State ───────────────────────────────────────────────────
enum class ReaderTheme { LIGHT, DARK, SEPIA }

@Composable
fun MyReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MyReaderTypography,
        content     = content
    )
}

@Composable
fun ReaderTheme(
    readerTheme: ReaderTheme = ReaderTheme.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (readerTheme) {
        ReaderTheme.DARK  -> DarkColorScheme
        ReaderTheme.SEPIA -> lightColorScheme(
            primary    = SepiaText,
            background = SepiaBackground,
            surface    = SepiaSurface,
            onBackground = SepiaText,
            onSurface    = SepiaText,
        )
        ReaderTheme.LIGHT -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MyReaderTypography,
        content     = content
    )
}
