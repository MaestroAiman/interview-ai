package com.pfa.interview.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = TextPrimary,
    primaryContainer = NavyCard,
    onPrimaryContainer = BlueLight,
    secondary = Emerald,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = EmeraldLight,
    tertiary = Amber,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = NavyCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    error = Rose,
    onError = TextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = TextPrimary,
    secondary = Emerald,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = NavyCard,
    onSurface = TextPrimary,
)

@Composable
fun InterviewSimulatorTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
