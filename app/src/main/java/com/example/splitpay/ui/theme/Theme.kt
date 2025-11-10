package com.example.splitpay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextWhite,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = TextWhite,

    secondary = PrimaryBlue,
    onSecondary = TextWhite,

    tertiary = PrimaryBlue,
    onTertiary = TextWhite,

    background = DarkBackground,
    onBackground = TextWhite,

    surface = DialogBackground,
    onSurface = TextWhite,
    surfaceVariant = DialogBackground,
    onSurfaceVariant = TextPlaceholder,

    error = ErrorRed,
    onError = TextWhite,
    errorContainer = ErrorRed,
    onErrorContainer = TextWhite,

    outline = BorderGray,
    outlineVariant = BorderGray,

    scrim = DarkBackground,

    inverseSurface = TextWhite,
    inverseOnSurface = DarkBackground,
    inversePrimary = PrimaryBlue,

    surfaceTint = PrimaryBlue,
)

@Composable
fun SplitPayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
