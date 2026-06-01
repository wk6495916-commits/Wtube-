package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TikTokNeonPink,
    secondary = TikTokCyan,
    tertiary = TikTokGold,
    background = TikTokDarkBackground,
    surface = TikTokCardBackground,
    onPrimary = TikTokWhite,
    onSecondary = TikTokDarkBackground,
    onTertiary = TikTokDarkBackground,
    onBackground = TikTokWhite,
    onSurface = TikTokWhite,
    surfaceVariant = TikTokGray,
    onSurfaceVariant = TikTokSubText
)

// Wtube looks and feels best in a clean, immersive dark environment
private val LightColorScheme = darkColorScheme(
    primary = TikTokNeonPink,
    secondary = TikTokCyan,
    tertiary = TikTokGold,
    background = TikTokDarkBackground,
    surface = TikTokCardBackground,
    onPrimary = TikTokWhite,
    onSecondary = TikTokDarkBackground,
    onBackground = TikTokWhite,
    onSurface = TikTokWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark-by-default to ensure cinematic immersive video playing!
    dynamicColor: Boolean = false, // Disable standard dynamic colors to keep brand brand-accurate
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
