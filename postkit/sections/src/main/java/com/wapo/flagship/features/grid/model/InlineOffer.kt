package com.wapo.flagship.features.grid.model

import com.wapo.android.commons.iterable.AttributionInfo

data class InlineOffer(
    val itemId: String?,
) : Item()

data class SectionInlineMessage(
    val attributionInfo: AttributionInfo?,
    val title: String?,
    val body: String?,
    val url: String?,
    val action: String?,
    val isEligiblePromo: Boolean?,
    val isAdFreeProduct: Boolean = false,
)
