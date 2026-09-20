package com.wapo.flagship.views.coordinator

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.wapo.flagship.common.map

class Coordinator(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : FrameLayout(
        context,
        attrs,
        defStyleAttr,
    ) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    val TAG = "Coordinator"
    lateinit var topBar: View
    lateinit var bottomBar: View
    lateinit var content: View
    private var startY: Float? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        topBar = findViewWithTag("topbar")
        bottomBar = findViewWithTag("bottombar")
        content = findViewWithTag("content")
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)
        content.translationY = topBar.height.toFloat()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val r = super.dispatchTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                if (startY != null) {
                    val minY = -topBar.height.toFloat()
                    val maxY = 0f
                    val delta = ev.y - startY as Float
                    var nextY = topBar.translationY + delta
                    nextY = nextY.coerceIn(minY, maxY)
                    topBar.translationY = nextY
                    content.translationY = topBar.translationY + topBar.height
                    bottomBar.translationY = nextY.map(maxY, minY, 0f, bottomBar.height.toFloat())
                }
                startY = ev.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                startY = null
            }
        }
        return r
    }
}
