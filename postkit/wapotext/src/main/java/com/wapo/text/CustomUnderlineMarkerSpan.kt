package com.wapo.text

import android.graphics.Color
import android.os.Build
import android.text.TextPaint
import android.text.style.CharacterStyle

class CustomUnderlineMarkerSpan(
    private val underlineColor: Int,
) : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint) {
        tp.bgColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tp.isUnderlineText = true
            tp.underlineColor = underlineColor
            tp.underlineThickness = 3f
        } else {
            tp.isUnderlineText = true
        }
    }
}
