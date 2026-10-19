package com.sijan.kharcha.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalKharchaColors = staticCompositionLocalOf { LightKharchaSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = PrimaryForestGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryForestGreenLight,
    onPrimaryContainer = Color(0xFF104B38),
    secondary = SecondarySlate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E8DE),
    onSecondaryContainer = Color(0xFF111F19),
    tertiary = TertiaryBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5EEF8),
    onTertiaryContainer = Color(0xFF0F2025),
    background = Color(0xFFF7F8F5),
    onBackground = Color(0xFF1C2420),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C2420),
    surfaceVariant = Color(0xFFEDF1EC),
    onSurfaceVariant = Color(0xFF536159),
    outline = Color(0xFF737D76),
    error = ErrorRedLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF78D5B0),
    onPrimary = Color(0xFF073B2A),
    primaryContainer = Color(0xFF204D3D),
    onPrimaryContainer = Color(0xFFC5F2DC),
    secondary = Color(0xFFB8C5BB),
    onSecondary = Color(0xFF24342C),
    secondaryContainer = Color(0xFF3B4A41),
    onSecondaryContainer = Color(0xFFD4E2D8),
    tertiary = Color(0xFFA5D1DE),
    onTertiary = Color(0xFF1B353D),
    tertiaryContainer = Color(0xFF334B53),
    onTertiaryContainer = Color(0xFFCEECF8),
    background = Color(0xFF121714),
    onBackground = Color(0xFFE3EAE4),
    surface = Color(0xFF1A211D),
    onSurface = Color(0xFFE3EAE4),
    surfaceVariant = Color(0xFF29342D),
    onSurfaceVariant = Color(0xFFB8C5BB),
    outline = Color(0xFF89958C),
    error = ErrorRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun KharchaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val semanticColors = if (darkTheme) DarkKharchaSemanticColors else LightKharchaSemanticColors

    CompositionLocalProvider(
        LocalKharchaColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object KharchaThemeDefaults {
    val colors: KharchaSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKharchaColors.current
}
