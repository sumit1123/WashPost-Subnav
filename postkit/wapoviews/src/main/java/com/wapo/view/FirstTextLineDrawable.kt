package com.wapo.view

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView


/**
 * A wrapper that aligns the given drawable at the center of the first line of the given TextView.
 * This drawable is designed to be used as a Compound Drawable with te given TextView.
 */
class FirstTextLineDrawable(val src: Drawable, val textView: FlowableTextView) : Drawable() {

    private val r = Rect()

    init {
        src.setBounds(0, 0, src.intrinsicWidth, src.intrinsicHeight)
    }

    override fun setAlpha(alpha: Int) {
        src.alpha = alpha
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return src.opacity
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        src.colorFilter = colorFilter
    }

    override fun getIntrinsicWidth(): Int {
        return src.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return src.intrinsicHeight
    }

    override fun draw(canvas: Canvas) {
        val halfCanvas = canvas.height / 2f
        val halfDrawable = src.intrinsicHeight / 2f
        r.setEmpty()
        getTextView().getLineBounds(0, r)
        val lineHeight = (r.bottom - r.top - getTextView().paint.fontMetrics.descent) / 2

        canvas.save()
        val layout = getTextView().layout
        val left = layout?.getLineLeft(0) ?: 0f
        canvas.translate(left, -halfCanvas + halfDrawable + lineHeight)
        src.draw(canvas)
        canvas.restore()
    }

    private fun getTextView(): TextView {
        return if (textView.sideText.text.toString().isNotEmpty() && textView.sideText.visibility == View.VISIBLE) textView.sideText else textView.centerText
    }
}