package com.wapo.view.tooltip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.core.content.ContextCompat
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.wapo.view.R

/**
 * Created by adkinsj on 11/14/18.
*/
class TooltipPopupOverlay(context: Context) : AppCompatImageView(context) {

    private var circleRect: RectF? = null
    private var radius: Float = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setCircle(rectF: RectF, radius: Float) {
        this.circleRect = rectF
        this.radius = radius
        //Redraw after defining circle
        postInvalidate()
    }

    fun setRectangle(rectF: RectF) {
        this.circleRect = rectF
        this.radius = 0f
        //Redraw after defining circle
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // overlay
        paint.color = ContextCompat.getColor(context, R.color.tooltip_overlay)
        paint.style = Paint.Style.FILL
        canvas?.drawPaint(paint)

        // gap
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        circleRect?.let { canvas?.drawRoundRect(it, radius, radius, paint) }
    }
}