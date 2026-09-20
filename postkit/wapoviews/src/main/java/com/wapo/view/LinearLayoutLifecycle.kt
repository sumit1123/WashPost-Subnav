package com.wapo.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

abstract class LinearLayoutLifecycle(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    final override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            onResume()
        } else {
            onPause()
        }
    }

    final override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            onResume()
        } else {
            onPause()
        }
    }

    final override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDestroy()
    }

    final override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onCreate()
    }

    abstract fun onCreate()

    abstract fun onPause()

    abstract fun onResume()

    abstract fun onDestroy()
}