package com.wapo.flagship.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar

class MakeSpaceForSnackbarBehavior : AppBarLayout.ScrollingViewBehavior {
    constructor() : super()
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean =
        dependency is Snackbar.SnackbarLayout ||
            super.layoutDependsOn(
                parent,
                child,
                dependency,
            )

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean {
        if (dependency is Snackbar.SnackbarLayout) {
            child.setPadding(0, 0, 0, dependency.height)
        }
        return super.onDependentViewChanged(parent, child, dependency)
    }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ) {
        if (dependency is Snackbar.SnackbarLayout) {
            child.setPadding(0, 0, 0, 0)
        }
        super.onDependentViewRemoved(parent, child, dependency)
    }
}
