package com.shaaztechno.videoplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricGreen,
    secondary = ElectricGreen,
    tertiary = Gray,
    background = Black,
    surface = SurfaceDark,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = LightGray,
    onSurface = LightGray,
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricGreen,
    secondary = ElectricGreen,
    tertiary = Gray,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Color.White,
    onBackground = Black,
    onSurface = Black,
)

@Composable
fun SZPlayerTheme(
    darkMode: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (darkMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Set system bars to transparent to leverage edge-to-edge drawing
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()

                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.isAppearanceLightStatusBars = !isDark
                windowInsetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // This refers to the Typography val in Typography.kt
        content = content
    )
}
