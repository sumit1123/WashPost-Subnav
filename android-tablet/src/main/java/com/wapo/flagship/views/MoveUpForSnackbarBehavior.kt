package com.wapo.flagship.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import kotlin.math.min

open class MoveUpForSnackbarBehavior : CoordinatorLayout.Behavior<View> {
    constructor() : super()
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean = dependency is SnackbarLayout

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean =
        when (dependency) {
            is SnackbarLayout -> {
                val translationY = min(0f, dependency.translationY - dependency.height)
                child.translationY = translationY
                true
            }
            else -> false
        }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ) {
        when (dependency) {
            is SnackbarLayout -> {
                child.animate().translationY(0f).start()
            }
        }
    }
}
