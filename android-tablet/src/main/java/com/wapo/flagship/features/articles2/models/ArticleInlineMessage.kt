package com.wapo.flagship.features.articles2.models

import com.wapo.android.commons.iterable.AttributionInfo

data class ArticleInlineMessage(
    val attributionInfo: AttributionInfo?,
    val title: String?,
    val body: String?,
    val url: String?,
    val action: String?,
    val isEligiblePromo: Boolean?,
    val isAdFreeProduct: Boolean = false,
)
