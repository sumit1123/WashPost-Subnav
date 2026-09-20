package com.washingtonpost.android.paywall.bottomsheet

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.washingtonpost.android.paywall.bottomsheet.ui.BottomCtaView

class PaywallSheetBehavior(context: Context, attrs: AttributeSet?) : BottomSheetBehavior<BottomCtaView>(context, attrs) {

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: BottomCtaView, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        val rv = target as? RecyclerView
        if (dyConsumed < 0 || rv != null && isNsvScrolledToBottom(rv)) {
            state = STATE_COLLAPSED
        } else if (dyConsumed > 0) {
            state = STATE_HIDDEN
        }
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }

    private fun isNsvScrolledToBottom(nsv: RecyclerView): Boolean {
        return !nsv.canScrollVertically(1)
    }
}