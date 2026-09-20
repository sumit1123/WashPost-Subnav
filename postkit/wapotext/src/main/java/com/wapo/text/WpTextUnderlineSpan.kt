package com.wapo.text

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan
import androidx.annotation.ColorRes
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class WpTextUnderlineSpan(@NonNull val context: Context, @NonNull @ColorRes val colorResId: Int,
                          val leftPadding: Float = 0f, val rightPadding: Float = 0f) : ReplacementSpan() {

    enum class Position {
        BASELINE, DESCENT, BOTTOM
    }

    private val color: Int = ContextCompat.getColor(context, colorResId)

    // underline default height is 1dp.
    // TextView should have enough space at the bottom (paddingBottom=1dp) to support given underline height.
    var height: Float = Resources.getSystem().displayMetrics.density * 1f

    var position: Position = Position.DESCENT

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fontMetrics: Paint.FontMetricsInt?): Int {
        return paint.measureText(text, start, end).roundToInt()
    }

    override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        if (text != null && text.isNotEmpty()) {
            // draw text
            canvas.drawText(text, start, end, x, y.toFloat(), paint)

            // calculate lineY for underline
            val yOffset = when (position) {
                Position.BASELINE -> 0f
                Position.DESCENT -> paint.fontMetrics.descent
                Position.BOTTOM -> paint.fontMetrics.bottom
            }
            val lineY = y + yOffset

            // draw line
            paint.color = color

            canvas.drawRect(RectF(x + leftPadding, lineY, x + paint.measureText(text, start, end) + rightPadding, lineY + height), paint)

        }
    }
}