package com.wapo.view.design.widget

import android.animation.ValueAnimator
import android.content.Context
import com.google.android.material.appbar.AppBarLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class AppBarBehavior(context: Context?, attrs: AttributeSet?) : AppBarLayout.Behavior(context, attrs) {

    private var mAnimator : ValueAnimator? = null
    private var mNestedScrollStarted = false

    override fun onStartNestedScroll(parent: CoordinatorLayout, child: AppBarLayout, directTargetChild: View, target: View, nestedScrollAxes: Int, type: Int): Boolean {
        mNestedScrollStarted = super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type)
        if (mNestedScrollStarted){
            mAnimator?.cancel()
        }
        return mNestedScrollStarted
    }

    @Deprecated("Deprecated in Java")
    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: AppBarLayout, target: View) {
        super.onStopNestedScroll(coordinatorLayout, child, target)

        if (!mNestedScrollStarted){
            return
        }

        mNestedScrollStarted = false

        val scrollRange = child.totalScrollRange
        val topOffset = topAndBottomOffset

        if (topOffset <= -scrollRange || topOffset >= 0) {
            // Already fully visible or fully invisible
            return;
        }

        if (topOffset < -(scrollRange / 2f)) {
            // Snap up (to fully invisible)
            animateOffsetTo(-scrollRange)
        } else {
            // Snap down (to fully visible)
            animateOffsetTo(0)
        }
    }

    fun  animateOffsetTo(offset : Int) {
        if (mAnimator == null) {
            val animator = ValueAnimator()
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                topAndBottomOffset = value
            }
            mAnimator = animator
        } else {
            mAnimator?.cancel()
        }

        mAnimator?.setIntValues(topAndBottomOffset, offset)
        mAnimator?.start()
    }
}