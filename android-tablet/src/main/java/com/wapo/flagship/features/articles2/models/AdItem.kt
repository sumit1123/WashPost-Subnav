package com.wapo.flagship.features.articles2.models

import com.wapo.flagship.features.articles.AdViewInfo

data class AdItem(
    override val type: String? = "AdType",
    val adKey: AdViewInfo? = null,
) : Item(type)
