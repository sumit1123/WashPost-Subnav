package com.wapo.flagship.sliding

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.wapo.view.R

class PullDownDelegate {
    private val GESTURE_THRESHOLD = 10
    private lateinit var screenSize: Point
    private lateinit var windowScrim: ColorDrawable
    private lateinit var root: View
    private var startX = 0f
    private var startY = 0f
    private var isSliding = false
    private lateinit var activity: PullDownActivity
    var animationDuration = 300L
    private var rootX = 0f
    private var rootY = 0f
    private var rootHeight = 0

    fun install(activity: PullDownActivity, root: View) {
        activity as Activity
        this.root = root
        this.activity = activity
        screenSize = Point().apply { activity.windowManager.defaultDisplay.getSize(this) }
        rootHeight = screenSize.y
        val statusBarColor = getColorByAttributeId(activity, R.attr.status_bar_color)
        setStatusBarColor(statusBarColor)
        windowScrim = ColorDrawable(statusBarColor)
        windowScrim.alpha = 0
        activity.window.setBackgroundDrawable(windowScrim)
    }

    @ColorInt
    private fun getColorByAttributeId(context: Context, @AttrRes attrIdForColor: Int): Int {
        val typedValue = TypedValue()
        val theme: Resources.Theme = context.theme
        theme.resolveAttribute(attrIdForColor, typedValue, true)
        return typedValue.data
    }

    private fun setStatusBarColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (activity as Activity).window.statusBarColor = color
        }
    }

    fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        var handled = false
        if (ev == null) return handled
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                if ((shouldHandle(startX, startY, ev) && activity.canSlideDown()) || isSliding) {
                    handled = true
                    if (!isSliding) {
                        isSliding = true
                        activity.onSlidingStarted()
                        ev.action = MotionEvent.ACTION_CANCEL
                        handled = false // let the activity to dispatch this event
                    }
                    root.y = (ev.y - startY).coerceAtLeast(rootY)
                    updateScrim()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isSliding) {
                    isSliding = false
                    activity.onSlidingFinished()
                    handled = true
                    if (shouldClose(ev.y - startY)) {
                        closeDownAndDismiss()
                    } else {
                        ObjectAnimator.ofFloat(root, "y", rootY).apply {
                            duration = animationDuration
                            start()
                        }
                    }
                }
                startX = rootX
                startY = rootY
            }
        }
        return handled
    }

    private fun shouldHandle(startX: Float, startY: Float, ev: MotionEvent): Boolean {
        val deltaX = (startX - ev.x).abs()
        if (deltaX > GESTURE_THRESHOLD) return false
        val deltaY = ev.y - startY
        return deltaY > GESTURE_THRESHOLD
    }

    private fun updateScrim() {
        val progress = root.y / rootHeight
        val alpha = (progress * 255f).toInt()
        windowScrim.alpha = 255 - alpha
    }

    private fun shouldClose(delta: Float): Boolean {
        return delta > rootHeight / 3
    }

    fun onPost(view: View) {
        rootX = view.x
        rootY = view.y
        rootHeight = view.measuredHeight
    }

    private fun closeDownAndDismiss() {
        setStatusBarColor(Color.TRANSPARENT)
        val start = root.y
        val finish = rootHeight.toFloat()
        val positionAnimator = ObjectAnimator.ofFloat(root, "y", start, finish)
        positionAnimator.duration = animationDuration
        positionAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                root.y = rootHeight.toFloat()
                updateScrim()
                (activity as Activity).finish()
            }

            override fun onAnimationCancel(animation: Animator) {
                updateScrim()
            }

            override fun onAnimationStart(animation: Animator) {
            }

        })
        positionAnimator.addUpdateListener {
            updateScrim()
        }
        positionAnimator.start()
    }

    private fun Float.abs(): Float {
        return if (this >= 0) this else -this
    }
}