package com.washingtonpost.android.recirculation.carousel.views

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

class CarouselRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var touchdownPoint: PointF = PointF(0f, 0f)
    var consumeTouchEventRule: CarouselView.CarouselConsumeTouchEventRule? = null
    var centerView: (() -> Unit)? = null

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        when (e?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchdownPoint = PointF(e.x, e.y)
                parent?.requestDisallowInterceptTouchEvent(false)
                if (canConsumeTouchEvent()) {
                    findViewPager(this)?.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(e.x - touchdownPoint.x)
                val dy = abs(e.y - touchdownPoint.y)
                if (dx > dy) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                centerView?.invoke()
            }
        }
        return super.onInterceptTouchEvent(e)
    }

    private fun findViewPager(view: View): ViewPager? {
        var parent = view.parent as? View
        while (parent is View) {
            if (parent is ViewPager) return parent
            parent = parent.parent as? View
        }
        return null
    }

    private fun canConsumeTouchEvent() =
        consumeTouchEventRule?.canCarouselConsumeTouchEvent() == true
}