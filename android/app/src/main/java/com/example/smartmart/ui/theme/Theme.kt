package com.example.smartmart.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SmartMartColorScheme = lightColorScheme(
    primary = SmartMartGreen,
    onPrimary = SmartMartSurface,
    primaryContainer = SmartMartLightGreen,
    onPrimaryContainer = SmartMartPressedGreen,
    secondary = SmartMartPressedGreen,
    onSecondary = SmartMartSurface,
    secondaryContainer = SmartMartLightGreen,
    onSecondaryContainer = SmartMartPressedGreen,
    background = SmartMartBackground,
    onBackground = SmartMartText,
    surface = SmartMartSurface,
    onSurface = SmartMartText,
    surfaceVariant = SmartMartBackground,
    onSurfaceVariant = SmartMartSecondaryText,
    surfaceContainer = SmartMartBackground,
    surfaceContainerLow = SmartMartSurface,
    outline = SmartMartBorder,
    outlineVariant = SmartMartBorder,
    error = SmartMartError,
    onError = Color.White,
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = SmartMartError,
)

/**
 * SWA-36 — Lê Tuấn Thành. Sprint 1 specifies a fixed light palette.
 * Existing parameters remain source-compatible; dark/dynamic palettes are not enabled
 * until the team supplies the corresponding design tokens.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SmartMartTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SmartMartColorScheme,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(12.dp),
        ),
        content = content,
    )
}
