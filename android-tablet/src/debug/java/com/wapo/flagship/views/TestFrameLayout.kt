package com.wapo.flagship.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout

open class TestFrameLayout : FrameLayout {
    private val paint: Paint
    var measureTime: Long = 0

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleRes: Int = 0) : super(
        context,
        attrs,
        defStyleRes,
    ) {
        paint =
            Paint().apply {
                color = Color.BLUE
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        Thread.sleep(measureTime)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawLine(0f, 0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), 0f, paint)
    }
}
