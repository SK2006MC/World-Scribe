package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SleekColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    secondary = SleekSecondary,
    onSecondary = SleekOnSecondary,
    tertiary = EmeraldTertiary,
    background = SleekBg,
    onBackground = SleekText,
    surface = SleekSurface,
    onSurface = SleekOnSurface,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekOnSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    forceDark: Boolean = false,
    content: @Composable () -> Unit
) {
    // For the "Sleek Interface" design theme, we use the beautiful light warm SleekColorScheme uniformly
    val colorScheme = SleekColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
