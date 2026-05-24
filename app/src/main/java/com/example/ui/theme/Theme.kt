package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = KineticFlame,
    secondary = CyberNeon,
    tertiary = NeonCyan,
    background = ObsidianBg,
    surface = DarkSlate,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = PremiumWhite,
    onSurface = PremiumWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for consistent premium branding
    dynamicColor: Boolean = false, // Stop dynamic colors to preserve our cohesive brand experience
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
