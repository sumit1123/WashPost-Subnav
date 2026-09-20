package com.wapo.text

import android.annotation.SuppressLint
import android.content.Context
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes

/**
 * Utility class to measure text without inflating a TextView
 */
class TextMeasure {

    /**
     * @param charSequence Source text
     * @param width width of the text box to put in, in pixels
     * @param styleRes Style for the text. Can include textSize, lineSpacingExtra, lineSpacingMultiplier, and fontFamily
     * @return measured height of the text, in pixels
     */
    @SuppressLint("ResourceType")
    fun measureTextHeight(
        charSequence: CharSequence,
        width: Int,
        context: Context,
        @StyleRes styleRes: Int
    ): Int {

        val attrs = intArrayOf(
            android.R.attr.textSize,
            android.R.attr.lineSpacingExtra,
            android.R.attr.lineSpacingMultiplier,
            android.R.attr.fontFamily
        )

        context.withStyledAttributes(styleRes, attrs) {
            val textSize = getDimension(0, 0f)
            val lineSpacingExtra = getDimension(1, 0f)
            val lineSpacingMultiplier = getDimension(2, 0f)
            val fontResourceId = getResourceId(3, 0)
            val typeface = if (fontResourceId > 0) {
                ResourcesCompat.getFont(context, fontResourceId)
            } else {
                null
            }
            val textPaint = TextPaint()
            textPaint.isAntiAlias = true
            textPaint.textSize = textSize
            textPaint.typeface = typeface
            val staticLayout = StaticLayout(
                charSequence,
                textPaint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                lineSpacingMultiplier,
                lineSpacingExtra,
                true
            )
            return staticLayout.height * staticLayout.lineCount
        }
        return 0
    }
}