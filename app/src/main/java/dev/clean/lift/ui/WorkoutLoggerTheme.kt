package dev.clean.lift.ui

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Palettes (owner, 2026-11-19). Each has light and dark sets covering every role the app uses,
 * including surface-container layers. Text-on-fill pairs are chosen for readable contrast:
 * white on strong colors, near-black on pale containers.
 */

private val IronLight = lightColorScheme(
    primary = Color(0xFFA9400F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCC), onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF4A6075), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E4F8), onSecondaryContainer = Color(0xFF061D2E),
    tertiary = Color(0xFF725C00), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE17A), onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFF8F7F6), onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFF8F7F6), onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFE4E1DE), onSurfaceVariant = Color(0xFF484542),
    outline = Color(0xFF7A7672), outlineVariant = Color(0xFFCBC6C1),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF2F1EF),
    surfaceContainer = Color(0xFFECEBE9), surfaceContainerHigh = Color(0xFFE6E5E3),
    surfaceContainerHighest = Color(0xFFE1DFDD), surfaceDim = Color(0xFFD9D7D5), surfaceBright = Color(0xFFF8F7F6),
    inverseSurface = Color(0xFF31302F), inverseOnSurface = Color(0xFFF3F0EE), inversePrimary = Color(0xFFFFB596),
)

private val IronDark = darkColorScheme(
    primary = Color(0xFFFFB596), onPrimary = Color(0xFF5A1B00),
    primaryContainer = Color(0xFF7F2C00), onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFB6C9DE), onSecondary = Color(0xFF203345),
    secondaryContainer = Color(0xFF37495C), onSecondaryContainer = Color(0xFFD3E4F8),
    tertiary = Color(0xFFE3C54A), onTertiary = Color(0xFF3C2F00),
    tertiaryContainer = Color(0xFF564500), onTertiaryContainer = Color(0xFFFFE17A),
    background = Color(0xFF151413), onBackground = Color(0xFFE6E2DF),
    surface = Color(0xFF151413), onSurface = Color(0xFFE6E2DF),
    surfaceVariant = Color(0xFF484542), onSurfaceVariant = Color(0xFFCBC6C1),
    outline = Color(0xFF959089), outlineVariant = Color(0xFF484542),
    surfaceContainerLowest = Color(0xFF100F0E), surfaceContainerLow = Color(0xFF1D1C1B),
    surfaceContainer = Color(0xFF21201F), surfaceContainerHigh = Color(0xFF2B2A29),
    surfaceContainerHighest = Color(0xFF363534), surfaceDim = Color(0xFF151413), surfaceBright = Color(0xFF3B3A38),
    inverseSurface = Color(0xFFE6E2DF), inverseOnSurface = Color(0xFF31302F), inversePrimary = Color(0xFFA9400F),
)

private val ChalkLight = lightColorScheme(
    primary = Color(0xFF6A4FA3), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF), onPrimaryContainer = Color(0xFF23005C),
    secondary = Color(0xFF006B5B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9EF2DC), onSecondaryContainer = Color(0xFF00201A),
    tertiary = Color(0xFF9A4521), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCD), onTertiaryContainer = Color(0xFF360F00),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EB), onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7A757F), outlineVariant = Color(0xFFCAC4CF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF8F2FA),
    surfaceContainer = Color(0xFFF2ECF4), surfaceContainerHigh = Color(0xFFECE6EE),
    surfaceContainerHighest = Color(0xFFE6E0E9), surfaceDim = Color(0xFFDED8E0), surfaceBright = Color(0xFFFFFBFF),
    inverseSurface = Color(0xFF322F35), inverseOnSurface = Color(0xFFF5EFF7), inversePrimary = Color(0xFFD1BCFF),
)

private val ChalkDark = darkColorScheme(
    primary = Color(0xFFD1BCFF), onPrimary = Color(0xFF3B1F71),
    primaryContainer = Color(0xFF523789), onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFF82D5C0), onSecondary = Color(0xFF00382E),
    secondaryContainer = Color(0xFF005143), onSecondaryContainer = Color(0xFF9EF2DC),
    tertiary = Color(0xFFFFB596), onTertiary = Color(0xFF581D00),
    tertiaryContainer = Color(0xFF7B2E0C), onTertiaryContainer = Color(0xFFFFDBCD),
    background = Color(0xFF141218), onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218), onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F), onSurfaceVariant = Color(0xFFCAC4CF),
    outline = Color(0xFF948F99), outlineVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F24), surfaceContainerHigh = Color(0xFF2B292F),
    surfaceContainerHighest = Color(0xFF36343A), surfaceDim = Color(0xFF141218), surfaceBright = Color(0xFF3B383E),
    inverseSurface = Color(0xFFE6E0E9), inverseOnSurface = Color(0xFF322F35), inversePrimary = Color(0xFF6A4FA3),
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF2E6A3F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB1F1BC), onPrimaryContainer = Color(0xFF00210B),
    secondary = Color(0xFF6E5D0E), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF9E287), onSecondaryContainer = Color(0xFF221B00),
    tertiary = Color(0xFF3A6470), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBEEAF7), onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFF6FBF3), onBackground = Color(0xFF181D18),
    surface = Color(0xFFF6FBF3), onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDDE5DA), onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717971), outlineVariant = Color(0xFFC1C9BF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF0F5ED),
    surfaceContainer = Color(0xFFEAEFE7), surfaceContainerHigh = Color(0xFFE4EAE2),
    surfaceContainerHighest = Color(0xFFDFE4DC), surfaceDim = Color(0xFFD7DBD4), surfaceBright = Color(0xFFF6FBF3),
    inverseSurface = Color(0xFF2D322C), inverseOnSurface = Color(0xFFEEF2EA), inversePrimary = Color(0xFF96D5A1),
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFF96D5A1), onPrimary = Color(0xFF003917),
    primaryContainer = Color(0xFF125129), onPrimaryContainer = Color(0xFFB1F1BC),
    secondary = Color(0xFFDCC66E), onSecondary = Color(0xFF3A3000),
    secondaryContainer = Color(0xFF544600), onSecondaryContainer = Color(0xFFF9E287),
    tertiary = Color(0xFFA2CEDB), onTertiary = Color(0xFF033541),
    tertiaryContainer = Color(0xFF214C58), onTertiaryContainer = Color(0xFFBEEAF7),
    background = Color(0xFF101510), onBackground = Color(0xFFDFE4DC),
    surface = Color(0xFF101510), onSurface = Color(0xFFDFE4DC),
    surfaceVariant = Color(0xFF414941), onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B9389), outlineVariant = Color(0xFF414941),
    surfaceContainerLowest = Color(0xFF0B0F0B), surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1C211C), surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerHighest = Color(0xFF313630), surfaceDim = Color(0xFF101510), surfaceBright = Color(0xFF353A34),
    inverseSurface = Color(0xFFDFE4DC), inverseOnSurface = Color(0xFF2D322C), inversePrimary = Color(0xFF2E6A3F),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Wallpaper colors need Android 12; older phones get the default palette. */
val wallpaperColorsAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun colorSchemeFor(context: Context, palette: Palette, dark: Boolean): ColorScheme =
    if (palette == Palette.WALLPAPER && wallpaperColorsAvailable) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else builtInScheme(palette, dark)

internal fun builtInScheme(palette: Palette, dark: Boolean): ColorScheme = when (palette) {
    Palette.WALLPAPER -> builtInScheme(Palette.DEFAULT, dark)
    Palette.IRON -> if (dark) IronDark else IronLight
    Palette.CHALK -> if (dark) ChalkDark else ChalkLight
    Palette.FOREST -> if (dark) ForestDark else ForestLight
}

@Composable
fun WorkoutLoggerTheme(dark: Boolean, palette: Palette, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorSchemeFor(LocalContext.current, palette, dark), typography = AppTypography, content = content)
}
