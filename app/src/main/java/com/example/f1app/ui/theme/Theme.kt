package com.example.f1app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary = RacingRed,
    onPrimary = Color.White,

    secondary = RacingRedDark,
    onSecondary = Color.White,

    background = TrackBlack,
    onBackground = TextPrimary,

    surface = TrackDark,
    onSurface = TextPrimary,

    surfaceVariant = TrackCard,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = RacingRedDark,
    onPrimary = Color.White,

    secondary = RacingRed,
    onSecondary = Color.White,

    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF181818),

    surface = Color.White,
    onSurface = Color(0xFF181818),

    surfaceVariant = Color(0xFFE5E7E9),
    onSurfaceVariant = Color(0xFF555A60)
)

@Composable
fun F1appTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            if (darkTheme) {
                window.statusBarColor = TrackBlack.toArgb()
                window.navigationBarColor = TrackBlack.toArgb()

                WindowCompat.setDecorFitsSystemWindows(window, true)

                WindowInsetsControllerCompat(window, view)
                    .isAppearanceLightStatusBars = false

                WindowInsetsControllerCompat(window, view)
                    .isAppearanceLightNavigationBars = false
            } else {
                val lightBackground = Color(0xFFF5F5F5)

                window.statusBarColor = lightBackground.toArgb()
                window.navigationBarColor = lightBackground.toArgb()

                WindowCompat.setDecorFitsSystemWindows(window, true)

                WindowInsetsControllerCompat(window, view)
                    .isAppearanceLightStatusBars = true

                WindowInsetsControllerCompat(window, view)
                    .isAppearanceLightNavigationBars = true
            }
        }
    }

    val baseTypography = Typography()

    val scaledTypography = baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(
            fontSize = baseTypography.displayLarge.fontSize * fontScale
        ),
        displayMedium = baseTypography.displayMedium.copy(
            fontSize = baseTypography.displayMedium.fontSize * fontScale
        ),
        displaySmall = baseTypography.displaySmall.copy(
            fontSize = baseTypography.displaySmall.fontSize * fontScale
        ),

        headlineLarge = baseTypography.headlineLarge.copy(
            fontSize = baseTypography.headlineLarge.fontSize * fontScale
        ),
        headlineMedium = baseTypography.headlineMedium.copy(
            fontSize = baseTypography.headlineMedium.fontSize * fontScale
        ),
        headlineSmall = baseTypography.headlineSmall.copy(
            fontSize = baseTypography.headlineSmall.fontSize * fontScale
        ),

        titleLarge = baseTypography.titleLarge.copy(
            fontSize = baseTypography.titleLarge.fontSize * fontScale
        ),
        titleMedium = baseTypography.titleMedium.copy(
            fontSize = baseTypography.titleMedium.fontSize * fontScale
        ),
        titleSmall = baseTypography.titleSmall.copy(
            fontSize = baseTypography.titleSmall.fontSize * fontScale
        ),

        bodyLarge = baseTypography.bodyLarge.copy(
            fontSize = baseTypography.bodyLarge.fontSize * fontScale
        ),
        bodyMedium = baseTypography.bodyMedium.copy(
            fontSize = baseTypography.bodyMedium.fontSize * fontScale
        ),
        bodySmall = baseTypography.bodySmall.copy(
            fontSize = baseTypography.bodySmall.fontSize * fontScale
        ),

        labelLarge = baseTypography.labelLarge.copy(
            fontSize = baseTypography.labelLarge.fontSize * fontScale
        ),
        labelMedium = baseTypography.labelMedium.copy(
            fontSize = baseTypography.labelMedium.fontSize * fontScale
        ),
        labelSmall = baseTypography.labelSmall.copy(
            fontSize = baseTypography.labelSmall.fontSize * fontScale
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
}