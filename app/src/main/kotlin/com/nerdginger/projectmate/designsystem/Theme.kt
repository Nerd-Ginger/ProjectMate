package com.nerdginger.projectmate.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Indigo = Color(0xFF4F5B93)
private val IndigoLight = Color(0xFFBAC3FF)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Color(0xFF5B5D72),
    tertiary = Color(0xFF77536D),
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF1F2A60),
    secondary = Color(0xFFC3C5DD),
    tertiary = Color(0xFFE6BAD7),
)

@Composable
fun ProjectMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You wallpaper colours, where the platform offers them.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
