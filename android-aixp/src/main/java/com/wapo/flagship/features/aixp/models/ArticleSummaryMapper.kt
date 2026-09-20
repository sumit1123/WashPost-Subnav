// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.features.aixp.models

/**
 * Helper class to map summary objects
 */
fun ArticleRealtimeSummary.toSummary(): ArticleSummary =
    ArticleSummary(
        url = url,
        title = null,
        disclaimer = disclaimer,
        keyPointsHeading = keyPointsHeading,
        overview = overview,
        keyPoints = keyPoints,
        modelId = modelId,
    )
