// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.topicfollow.fragments

import android.content.DialogInterface
import androidx.fragment.app.activityViewModels
import com.wapo.flagship.features.articles2.paywall.WallUiEvent
import com.wapo.flagship.features.articles2.viewmodels.ArticleWallHelperViewModel
import com.wapo.flagship.features.preferencesapi.models.Followable
import dagger.hilt.android.AndroidEntryPoint

/**
 * Class that extends [TopicFollowBottomSheetFragment] for use in articles.
 * The only difference is the inclusion of [ArticleWallHelperViewModel] to help with conflicts with other article walls
 */
@AndroidEntryPoint
class TopicFollowArticleBottomSheetFragment(
    contentPackId: String,
    followable: Followable,
    trackingPageName: String,
) : TopicFollowBottomSheetFragment(contentPackId, followable, trackingPageName) {
    private val articleWallHelperViewModel: ArticleWallHelperViewModel by activityViewModels()

    override fun onStart() {
        // This is to handle the edge cases where this sheet is displayed at the same time
        // paywall is displayed
        if (articleWallHelperViewModel.paywallEvent.value is WallUiEvent.ShowPaywall ||
            articleWallHelperViewModel.paywallEvent.value is WallUiEvent.ShowRegwall
        ) {
            dismiss()
        } else {
            // This is to handle scenarios where delayed paywall is stopped when this sheet is open
            articleWallHelperViewModel.dispatchStopDelayedPaywall()
        }
        super.onStart()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Delayed paywall should be started again when this sheet is dismissed.
        // logic of whether to show wall based on sub status is handled internally in func below
        articleWallHelperViewModel.dispatchShowPaywallDelayed()
        super.onDismiss(dialog)
    }
}
