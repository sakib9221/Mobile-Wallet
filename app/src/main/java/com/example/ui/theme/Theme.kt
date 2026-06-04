package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightTeal,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = TealPrimary,
    onPrimaryContainer = Color.White,
    secondary = LightOrange,
    onSecondary = Color(0xFF0F172A),
    tertiary = LightTeal,
    background = BackgroundDark,
    onBackground = Color(0xFFF8FAFC),
    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D)
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = TealDark,
    secondary = OrangeSecondary,
    onSecondary = Color.White,
    tertiary = TealDark,
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val LocalThemeState = androidx.compose.runtime.staticCompositionLocalOf { "system" }

@Composable
fun MyApplicationTheme(
    selectedTheme: String = "system",
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default dynamicColor to false to maintain cohesive brand identity on all devices
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (selectedTheme) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val resolvedTheme = if (selectedTheme == "liquid_glass") "system" else selectedTheme
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customDensity = androidx.compose.ui.unit.Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale.coerceAtMost(1.15f)
    )
    androidx.compose.runtime.CompositionLocalProvider(
        LocalThemeState provides resolvedTheme,
        androidx.compose.ui.platform.LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
