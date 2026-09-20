package com.wpds.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.wapo.flagship.features.nightmode.NightModeController

@Composable
fun AndroidClassicTheme(
    content: @Composable () -> Unit
) {
    val useDarkTheme =
        (LocalContext.current.applicationContext as? NightModeController)?.isNightModeEnabled()
        ?: isSystemInDarkTheme()
    
    val wpdsColorPalette = if (useDarkTheme) DarkColors else LightColors

    CompositionLocalProvider(WPDSColors provides wpdsColorPalette) {
        MaterialTheme(
            typography = Typography, // TODO update Type.kt and use that class here
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}
