package com.wapo.flagship.views

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.SwipeDismissBehavior

class BottomBarSwipeDismissBehavior(
    private val dependencyViewId: Int,
) : SwipeDismissBehavior<View>() {
    private var dependencyViewTop: Int? = null
    private var dependencyViewOffset: Int? = null

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean = dependency.id == dependencyViewId

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean {
        dependencyViewTop = dependency.top
        dependencyViewOffset = dependency.translationY.toInt()
        layoutView(child, dependencyViewTop as Int, dependencyViewOffset as Int, parent)
        return true
    }

    private fun layoutView(
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
        val navBarTop = dependencyViewTop
        val navBarOffset = dependencyViewOffset
        // if (child is Snackbar.SnackbarLayout && navBarOffset != null && navBarTop != null) {
        if (navBarOffset != null && navBarTop != null) {
            layoutView(child, navBarTop, navBarOffset, parent)
            return true
        }
        return super.onLayoutChild(parent, child, layoutDirection)
    }
}
