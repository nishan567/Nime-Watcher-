package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val NimeDarkColorScheme = darkColorScheme(
    primary = VibrantOrange,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepEmberContainer,
    onPrimaryContainer = SoftPeachGlow,
    secondary = WarmAmber,
    onSecondary = ObsidianBlack,
    secondaryContainer = CharcoalElevated,
    onSecondaryContainer = PureWhite,
    tertiary = EmberOrange,
    onTertiary = PureWhite,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = CoolSilver,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = CharcoalCard,
    surfaceContainerHigh = CharcoalElevated,
    outline = SlateBorder,
    outlineVariant = CharcoalElevated,
    error = CrimsonError,
    onError = ObsidianBlack,
    errorContainer = CrimsonContainer,
    onErrorContainer = PureWhite
)

private val NimeLightColorScheme = lightColorScheme(
    primary = VibrantOrange,
    onPrimary = PureWhite,
    primaryContainer = LightOrangeContainer,
    onPrimaryContainer = DeepRust,
    secondary = EmberOrange,
    onSecondary = PureWhite,
    secondaryContainer = SoftTintLight,
    onSecondaryContainer = InkBlack,
    tertiary = WarmAmber,
    onTertiary = InkBlack,
    background = CrispSnow,
    onBackground = InkBlack,
    surface = PureCardLight,
    onSurface = InkBlack,
    surfaceVariant = SoftTintLight,
    onSurfaceVariant = InkMuted,
    surfaceContainerLow = CrispSnow,
    surfaceContainer = PureCardLight,
    surfaceContainerHigh = SoftTintLight,
    outline = LightBorder,
    outlineVariant = SoftTintLight,
    error = CrimsonError,
    onError = PureWhite
)

object NimeGradients {
    val orangeHeroBrush = Brush.linearGradient(
        colors = listOf(EmberOrange, VibrantOrange, WarmAmber)
    )

    val cardGlowBorder = Brush.linearGradient(
        colors = listOf(
            VibrantOrange.copy(alpha = 0.65f),
            WarmAmber.copy(alpha = 0.25f),
            Color.Transparent
        )
    )

    val darkScrimBottom = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            ObsidianBlack.copy(alpha = 0.72f),
            ObsidianBlack
        )
    )
}

@Composable
fun NimeWatcherTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NimeDarkColorScheme else NimeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NimeTypography,
        shapes = NimeShapes,
        content = content
    )
}
