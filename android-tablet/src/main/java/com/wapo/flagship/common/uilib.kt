package com.wapo.flagship.common

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import com.wapo.android.commons.util.Logger
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.wapo.flagship.features.extentions.isTablet

fun Drawable.tint(color: Int): Drawable {
    val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    return this.mutate().apply { setColorFilter(colorFilter) }
}

/**
 * Performs the given action when this view lays out its children.
 * The action will only be invoked once
 */
inline fun ViewGroup.doOnChildLayout(crossinline action: (view: View) -> Unit) {
    addOnLayoutChangeListener(
        object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int,
            ) {
                if (childCount > 0) {
                    view.removeOnLayoutChangeListener(this)
                    action(view)
                }
            }
        },
    )
}

/**
 * Make ViewPager2 less sensitive to swiping
 */
fun ViewPager2.fixPagingGesture() {
    try {
        val rv = findChildRecursively { it is RecyclerView }
        if (rv != null) {
            val multiplier = if (context.isTablet()) 4 else 2 // chosen heuristically
            val fixedSwipeSlop = ViewConfiguration.get(this.context).scaledPagingTouchSlop * multiplier
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            touchSlopField.set(rv, fixedSwipeSlop)
        }
    } catch (t: Throwable) {
        Logger.e("ViewPager2", "error", t)
    }
}

fun ViewGroup.findChildRecursively(predicate: (View) -> Boolean): View? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (predicate(child)) {
            return child
        }
        if (child is ViewGroup) {
            val candidate = child.findChildRecursively(predicate)
            if (candidate != null) return candidate
        }
    }
    return null
}
