package com.smartjoy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFDBEAFE), 
    onPrimaryContainer = PrimaryDeepBlue,
    
    background = BackgroundSoftWhite,
    onBackground = OnBackgroundSlate,
    
    surface = SurfaceWhite,
    onSurface = OnSurfaceSlate,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = OnSurfaceVariantSlate,
    
    outline = OutlineSlate,
    
    error = DangerRed,
    onError = OnDanger,
    errorContainer = DangerContainer,
    onErrorContainer = DangerRed,

    tertiary = SafeGreen,
    onTertiary = OnSafe,
    tertiaryContainer = SafeContainer,
    onTertiaryContainer = SafeGreen,
    
    secondary = CautionAmber,
    onSecondary = OnCaution,
    secondaryContainer = CautionContainer,
    onSecondaryContainer = CautionAmber
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark
)

@Composable
fun SmartJoyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = LightColors 
    
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
