package com.example.retrovault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.retrovault.data.CornerStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalCornerStyle = staticCompositionLocalOf { CornerStyle.ROUNDED }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface
)

@Composable
fun RetroVaultTheme(
    darkTheme: Boolean = true,
    corners: CornerStyle = CornerStyle.ROUNDED,
    // Dynamic color is available on Android 12+, but disabled by default to maintain consistent Synthwave look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkColorScheme = darkColorScheme(
        primary = SynthwaveLavender,
        secondary = SynthwaveLavender,
        background = DarkBackground,
        surface = DarkSurface,
        onPrimary = SynthwaveDark,
        onSecondary = SynthwaveDark,
        onBackground = DarkOnBackground,
        onSurface = DarkOnSurface
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is Activity) break
                currentContext = currentContext.baseContext
            }
            
            val activity = currentContext as? Activity
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                window.navigationBarColor = Color.Transparent.toArgb()
                window.statusBarColor = Color.Transparent.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalCornerStyle provides corners
        ) {
            content()
        }
    }
}
