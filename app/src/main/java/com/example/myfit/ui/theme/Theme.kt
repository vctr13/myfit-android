package com.example.myfit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary              = Blue40,
    onPrimary            = Color.White,
    primaryContainer     = Blue90,
    onPrimaryContainer   = Blue10,
    secondary            = Teal40,
    onSecondary          = Color.White,
    secondaryContainer   = Teal90,
    onSecondaryContainer = Color(0xFF00201C),
    tertiary             = Green40,
    onTertiary           = Color.White,
    tertiaryContainer    = Green90,
    onTertiaryContainer  = Green10,
    background           = NeutralBg,
    surface              = Color(0xFFFBFFFB),
    onBackground         = Color(0xFF191C20),
    onSurface            = Color(0xFF191C20),
    surfaceVariant       = Color(0xFFE2E5F1),
    onSurfaceVariant     = Color(0xFF45474F),
    outline              = Color(0xFF757780),
    outlineVariant       = Color(0xFFC5C6D0),
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    surfaceContainerLowest  = SurfaceContainerLowest,
    surfaceContainerLow     = SurfaceContainerLow,
    surfaceContainer        = SurfaceContainerMid,
    surfaceContainerHigh    = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Blue80,
    onPrimary            = Blue20,
    primaryContainer     = Blue30,
    onPrimaryContainer   = Blue90,
    secondary            = Teal80,
    onSecondary          = Color(0xFF003731),
    secondaryContainer   = Color(0xFF005048),
    onSecondaryContainer = Teal90,
    tertiary             = Green80,
    onTertiary           = Green20,
    tertiaryContainer    = Green30,
    onTertiaryContainer  = Green90,
    background           = NeutralDark,
    surface              = SurfaceContainerLowestDark,
    onBackground         = Color(0xFFE2E2E9),
    onSurface            = Color(0xFFE2E2E9),
    surfaceVariant       = Color(0xFF45474F),
    onSurfaceVariant     = Color(0xFFC5C6D0),
    outline              = Color(0xFF8F9099),
    outlineVariant       = Color(0xFF45474F),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    surfaceContainerLowest  = SurfaceContainerLowestDark,
    surfaceContainerLow     = SurfaceContainerLowDark,
    surfaceContainer        = SurfaceContainerMidDark,
    surfaceContainerHigh    = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

@Composable
fun MyFITTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
