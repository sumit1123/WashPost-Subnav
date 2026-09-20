package com.wapo.flagship.features.grid.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.washingtonpost.android.sections.R

class UnderlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {
    private val underlineColor: Int
    private val underlineStrokeWith: Float
    private val paint: Paint
    private var isUnderlined: Boolean = false

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.UnderlinedTextView,
            0,
            0
        ).apply {
            try {
                underlineColor = getColor(
                    R.styleable.UnderlinedTextView_underline_color,
                    currentTextColor
                )
                underlineStrokeWith = getDimension(
                    R.styleable.UnderlinedTextView_stroke_width,
                    resources.displayMetrics.density
                )
                paint = Paint().apply {
                    color = underlineColor
                    strokeWidth = underlineStrokeWith
                    isAntiAlias = true
                }
            } finally {
                recycle()
            }
        }
    }

    fun setUnderlined(isUnderlined: Boolean) {
        if (this.isUnderlined == isUnderlined) return
        this.isUnderlined = isUnderlined
        invalidate()
        requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isUnderlined) {
            val y = height.toFloat() - (2f * resources.displayMetrics.scaledDensity)
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }
    }
}