package com.example.raitha_varta.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// These are the colors we defined in Color.kt
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    secondary = EarthyBrown,
    tertiary = ActionAmber,
    background = Color(0xFF121212), // Dark background for night mode
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = EarthyBrown,
    tertiary = ActionAmber,
    background = White,
    surface = White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun RaithaVartaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamicColor to ensure your specific green/amber theme
    // is always visible to the farmer regardless of their phone wallpaper.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}