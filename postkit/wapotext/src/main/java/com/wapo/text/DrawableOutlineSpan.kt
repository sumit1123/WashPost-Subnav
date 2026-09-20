package com.wapo.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ReplacementSpan

class DrawableOutlineSpan(
    private val drawable: Drawable,
) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        fm?.let {
            val originalAscent = paint.fontMetricsInt.ascent
            it.ascent = Math.max(originalAscent, -drawable.intrinsicHeight)

            it.descent = paint.fontMetricsInt.descent
        }

        return drawable.intrinsicWidth
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float, // X-coordinate for the start of this span
        top: Int, // Y-coordinate for the top of the entire line (after layout)
        y: Int, // Y-coordinate for the baseline of the text
        bottom: Int, // Y-coordinate for the bottom of the entire line (after layout)
        paint: Paint,
    ) {
        canvas.save()

        val totalHeight = bottom - top
        val chipHeight = drawable.intrinsicHeight
        val offset = (chipHeight * 0.06f).toInt()
        val yPosition = top + (totalHeight - chipHeight) / 2 - offset

        drawable.setBounds(
            x.toInt(),
            yPosition,
            x.toInt() + drawable.intrinsicWidth,
            yPosition + chipHeight
        )

        drawable.draw(canvas)
        canvas.restore()
    }
}
