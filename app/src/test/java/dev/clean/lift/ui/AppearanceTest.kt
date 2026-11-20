package dev.clean.lift.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {
    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance().toDouble(), b.luminance().toDouble()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test fun storedNamesFallBackToDefaultsWhenMissingOrUnknown() {
        assertEquals(Palette.IRON, Palette.fromName(null))
        assertEquals(Palette.IRON, Palette.fromName("NEON"))
        assertEquals(Palette.CHALK, Palette.fromName("CHALK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
        assertTrue(AppearanceStore.validPalette("FOREST") && !AppearanceStore.validPalette("forest"))
        assertTrue(AppearanceStore.validTheme("LIGHT") && !AppearanceStore.validTheme(""))
    }

    /** Readable text on every built-in fill: WCAG AA (4.5:1) for text roles the app uses. */
    @Test fun everyBuiltInPaletteKeepsReadableTextContrast() {
        for (palette in Palette.entries) for (dark in listOf(false, true)) {
            val s = builtInScheme(palette, dark)
            val pairs = mapOf(
                "primary" to (s.onPrimary to s.primary),
                "primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
                "secondary" to (s.onSecondary to s.secondary),
                "secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
                "tertiary" to (s.onTertiary to s.tertiary),
                "tertiaryContainer" to (s.onTertiaryContainer to s.tertiaryContainer),
                "surface" to (s.onSurface to s.surface),
                "surfaceVariantText" to (s.onSurfaceVariant to s.surface),
                "primaryOnSurface" to (s.primary to s.surface),
                "error" to (s.onError to s.error),
            )
            pairs.forEach { (role, colors) ->
                val ratio = contrast(colors.first, colors.second)
                assertTrue("$palette dark=$dark $role contrast $ratio", ratio >= 4.5)
            }
        }
    }
}
