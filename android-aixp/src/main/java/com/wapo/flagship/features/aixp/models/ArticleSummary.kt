// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.aixp.models

/**
 * UI Model class to map from
 * 1. Approved summaries from articles
 * 2. Realtime summaries from API fallback
 * Notes:
 * revisionId is from summary.id from feeds
 * modelId is from model_id from realtime summary
 */
data class ArticleSummary(
    val url: String?,
    val title: String?,
    val disclaimer: String?,
    val keyPointsHeading: String?,
    val overview: String?,
    val keyPoints: List<String?>?,
    val modelId: String?,
)
