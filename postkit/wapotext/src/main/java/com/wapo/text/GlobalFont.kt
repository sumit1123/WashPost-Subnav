package com.wapo.text

object GlobalFont {
    const val MIN_FONT_SIZE = -2
    const val MAX_FONT_SIZE = 4
    const val DEFAULT_FONT_SIZE = 0

    // [AppContext] sets this value(FONT_SIZE_MIN..FONT_SIZE_MAX) from Preferences.
    // Settings also set this value when user adjusts the Text Size or related preferences in Settings.
    var fontSizeAdjustment: Float = 0f
}