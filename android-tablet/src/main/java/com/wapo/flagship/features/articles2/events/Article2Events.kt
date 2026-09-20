package com.wapo.flagship.features.articles2.events

import com.wapo.flagship.features.articles2.tracking.PushArticleTrackingHelperData
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.util.tracking.ArticlePageViewTrackingData
import com.wapo.flagship.util.tracking.MeasurementMap

sealed interface Article2Events {
    data class Article2TrackerEvent(
        val omniture: TrackingInfo,
        val position: Int,
        val currentAppTab: String,
        val currentAppSection: String,
        val pushTrackingHelperData: PushArticleTrackingHelperData?,
        val measurementMap: MeasurementMap
    ) : Article2Events

    data class Article2PageViewEvent(
        val articlePageViewTrackingData: ArticlePageViewTrackingData
    ) : Article2Events
}