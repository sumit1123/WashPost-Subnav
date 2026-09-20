package com.wapo.flagship.features.backendhealth.models

import com.wapo.android.commons.util.timeAgo

data class FailoverState(
    val isLoading: Boolean = false,
    val fallbackURL: String? = null,
    val fallbackStaticURL: String? = null,
    val articles: List<FailoverArticle> = emptyList(),
    val healthCheckIsLoading: Boolean = false,
    val isHealthy: Boolean = true,
    val showFallbackPopUp: Boolean = false
)

data class FailoverArticle(
    val headline: String = "",
    val byline: String = "",
    val imageUrl: String? = null,
    val contentUrl: String,
    val publishTime: Long?,
) {
    val bylineText = when {
        publishTime != null && byline.isNotEmpty() -> "By ${byline} \u2022 ${timeAgo(publishTime)}"
        byline.isNotEmpty() -> "By $byline"
        publishTime != null -> timeAgo(publishTime)
        else -> ""
    }
}