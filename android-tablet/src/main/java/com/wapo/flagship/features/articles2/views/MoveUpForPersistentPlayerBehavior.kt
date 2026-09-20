package com.wapo.flagship.features.articles2.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.washingtonpost.android.R
import kotlin.math.min

class MoveUpForPersistentPlayerBehavior : CoordinatorLayout.Behavior<View> {
    constructor() : super()
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean {
        if (dependency.id == R.id.persistent_player_frame) {
            onDependentViewChanged(parent, child, dependency)
        }
        return dependency.id == R.id.persistent_player_frame
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View,
    ): Boolean =
        when {
            dependency is FrameLayout -> {
                if (dependency.visibility == View.VISIBLE) {
                    val bottomMargin = (child.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
                    val translationY =
                        min(
                            0f,
                            // Adding 16 to give the persistent player a little breathing room
                            dependency.translationY - (dependency.height + 16) + bottomMargin,
                        )
                    child.animate().translationY(translationY)
                } else {
                    child.animate().translationY(0f).start()
                }
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
            is FrameLayout -> {
                if (dependency.id == R.id.persistent_player_frame) {
                    child.animate().translationY(0f).start()
                }
            }
        }
    }
}
