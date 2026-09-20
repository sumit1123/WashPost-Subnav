/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.domain.repository

import com.wapo.android.remotelog.logger.EventTimerLog.ALERTS_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.ANDROID_AUTO_FOR_YOU_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.ANDROID_AUTO_PODCAST_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.ASK_QUESTION_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.ASK_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.COMICS_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.COMMENTS_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.FOR_YOU_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.PAYWALL_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.RECIPES_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.SEARCH_RESULT_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.SEARCH_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.SIGN_IN_SCREEN
import com.wapo.android.remotelog.logger.EventTimerLog.WATCH_SCREEN

interface LoadRenderMetrics {

    fun startLoadRenderMetrics(loadRenderEvent: LoadRenderMetricsEvent)

    fun stopLoadRenderMetrics(loadRenderEvent: LoadRenderMetricsEvent)

    fun stopLoadRenderMetrics(
        loadRenderEvent: LoadRenderMetricsEvent,
        additionalFields: Map<String, String>
    ) {
        stopLoadRenderMetrics(loadRenderEvent)
    }
}

sealed class LoadRenderMetricsEvent(val event: String) {

    data object SearchRenderEvent : LoadRenderMetricsEvent(SEARCH_SCREEN)
    data object SearchResultRenderEvent : LoadRenderMetricsEvent(SEARCH_RESULT_SCREEN)
    data object AskRenderEvent : LoadRenderMetricsEvent(ASK_SCREEN)
    data object WatchRenderEvent : LoadRenderMetricsEvent(WATCH_SCREEN)
    data object RecipesRenderEvent : LoadRenderMetricsEvent(RECIPES_SCREEN)
    data object AlertsRenderEvent : LoadRenderMetricsEvent(ALERTS_SCREEN)
    data object CommentsRenderEvent : LoadRenderMetricsEvent(COMMENTS_SCREEN)
    data object ForYouRenderEvent : LoadRenderMetricsEvent(FOR_YOU_SCREEN)
    data object PaywallRenderEvent : LoadRenderMetricsEvent(PAYWALL_SCREEN)
    data object SignInRenderEvent : LoadRenderMetricsEvent(SIGN_IN_SCREEN)
    data object ComicsRenderEvent : LoadRenderMetricsEvent(COMICS_SCREEN)
    data object AndroidAutoForYouRenderEvent : LoadRenderMetricsEvent(ANDROID_AUTO_FOR_YOU_SCREEN)
    data object AndroidAutoPodcastRenderEvent : LoadRenderMetricsEvent(ANDROID_AUTO_PODCAST_SCREEN)
    data object AskQuestionRenderEvent : LoadRenderMetricsEvent(ASK_QUESTION_SCREEN)

    companion object {
        @JvmField
        val PAYWALL_RENDER_EVENT: LoadRenderMetricsEvent = LoadRenderMetricsEvent.PaywallRenderEvent
    }
}
