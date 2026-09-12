package com.shaaztechno.videoplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun SZPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography, // This refers to the Typography val in Typography.kt
        content = content
    )
}
