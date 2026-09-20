package com.wapo.flagship.util.tracking

import com.wapo.flagship.features.articles2.tracking.PushArticleTrackingHelperData
import com.wapo.flagship.json.TrackingInfo

data class ArticlePageViewTrackingData(
    val omniture: TrackingInfo? = null,
    val position: Int = -1,
    val currentAppTab: String? = null,
    val currentAppSection: String? = null,
    val pushTrackingHelperData: PushArticleTrackingHelperData? = null,
    val measurementMap: MeasurementMap? = null
)