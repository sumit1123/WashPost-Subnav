package com.wapo.flagship.features.articles2.utils

import android.view.MotionEvent
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.aixp.SummariesFeatureFlag
import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel

internal class ScrollStopper(
    private val articlesPagerCollaborationViewModel: ArticlesPagerCollaborationViewModel,
) : RecyclerView.OnItemTouchListener {
    override fun onInterceptTouchEvent(
        @NonNull recyclerView: RecyclerView,
        @NonNull motionEvent: MotionEvent,
    ): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val summariesEnabled = SummariesFeatureFlag.shouldEnableSummariesIcon(
                articlesPagerCollaborationViewModel.currentPageSummaryAvailability.value?.first == true,
                articlesPagerCollaborationViewModel.currentPageSummaryAvailability.value?.second == true
            )
            if (articlesPagerCollaborationViewModel.summaryTooltipEvent.value == null &&
                summariesEnabled
            ) {
                articlesPagerCollaborationViewModel.dispatchSummaryTooltipEvent(true)
                recyclerView.parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        if (motionEvent.action == MotionEvent.ACTION_DOWN && recyclerView.getScrollState() === RecyclerView.SCROLL_STATE_SETTLING) {
            recyclerView.stopScroll()
        }
        return false
    }

    override fun onTouchEvent(
        rv: RecyclerView,
        e: MotionEvent,
    ) {}

    override fun onRequestDisallowInterceptTouchEvent(b: Boolean) {}
}
