package com.saathi.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryAmber,
    tertiary = SafeGreen,
    error = ErrorSOS,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = LightSurface,
    onSecondary = DarkBackground,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryAmber,
    tertiary = SafeGreen,
    error = ErrorSOS,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightSurface,
    onSecondary = DarkBackground,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary = HighContrastPrimary,
    secondary = HighContrastSecondary,
    tertiary = SafeGreen,
    error = HighContrastError,
    background = HighContrastBackgroundLight,
    surface = HighContrastSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = HighContrastOnBackgroundLight,
    onSurface = HighContrastOnBackgroundLight
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    secondary = HighContrastSecondary,
    tertiary = SafeGreen,
    error = HighContrastError,
    background = HighContrastBackgroundDark,
    surface = HighContrastSurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = HighContrastOnBackgroundDark,
    onSurface = HighContrastOnBackgroundDark
)

@Composable
fun SaathiTheme(
    themePreference: String = "System",
    isHighContrast: Boolean = false,
    isLargeText: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themePreference.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    val colorScheme = when {
        isHighContrast && isDark -> HighContrastDarkColorScheme
        isHighContrast && !isDark -> HighContrastLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = if (isLargeText) LargeTypography else Typography

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
