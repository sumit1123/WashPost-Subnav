package com.wpds.theme

import androidx.compose.runtime.mutableStateOf

/**
 * Compose-observable mirror of [com.wapo.text.GlobalFont.fontSizeAdjustment].
 *
 * [com.wapo.text.GlobalFont] is a plain Kotlin var and cannot trigger Compose recomposition.
 * This object wraps the same value as a [androidx.compose.runtime.MutableState] so that any
 * composable reading [adjustment] will automatically recompose when the user changes the
 * text-size setting in app Settings.
 *
 * Write sites:
 *  - [com.wapo.flagship.AppContext.init] — initializes at app startup
 *  - [com.wapo.flagship.features.settings.preferences.FontSizePreference.saveProgressAndUpdateView]
 *    — updates live when the user drags the font-size slider
 */
object ArticleFontScale {
    val adjustment = mutableStateOf(0f)

    fun setAdjustment(value: Float) {
        adjustment.value = value
    }
}

