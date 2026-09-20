package com.wapo.flagship.views

import android.content.Context
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlin.math.min

class SnackbarBehavior(
    context: Context?,
) : SwipeDismissBehavior<View>() {
    private var navBarTop: Int? = null
    private var navBarOffset: Int? = null

    override fun canSwipeDismissView(view: View): Boolean = view is Snackbar.SnackbarLayout

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean = dependency is BottomNavigationView

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean {
        navBarTop = dependency.top
        navBarOffset = min(dependency.translationY.toInt(), dependency.height)
        layoutSnackBar(child, navBarTop as Int, navBarOffset as Int, parent)
        return true
    }

    private fun layoutSnackBar(
        child: View,
        navBarTop: Int,
        navBarOffset: Int,
        parent: CoordinatorLayout,
    ) {
        val h = child.measuredHeight
        val top = navBarTop - h
        val w = child.measuredWidth
        val parentW = parent.measuredWidth
        val left = parentW / 2 - w / 2
        val right = left + w
        child.layout(left, top + navBarOffset, right, navBarTop + navBarOffset)
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: View,
        layoutDirection: Int,
    ): Boolean {
        val navBarTop = navBarTop
        val navBarOffset = navBarOffset
        if (child is Snackbar.SnackbarLayout && navBarOffset != null && navBarTop != null) {
            layoutSnackBar(child, navBarTop, navBarOffset, parent)
            return true
        }
        return super.onLayoutChild(parent, child, layoutDirection)
    }
}
