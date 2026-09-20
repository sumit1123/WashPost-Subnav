/*
 * Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.flagship.features.articles.recirculation.model

import com.google.gson.annotations.SerializedName

/**
 * Created by Jayesh Elamgodil on 08/24/19.
 */
data class MostReadFeed(
    @SerializedName("content_elements") val elements: List<MostReadElement>?,
)

data class MostReadElement(
    @SerializedName("canonical_url") val canonicalURL: String,
    @SerializedName("website") val website: String?,
    @SerializedName("headlines") val headlines: MostReadSubElement?,
    @SerializedName("label") val label: MRELabel?,
    @SerializedName("label_display") val labelDisplay: MRELabel?,
    @SerializedName("promo_items") val promoItems: MREPromoItems?,
    @SerializedName("credits") val credits: MRECredits?,
)

data class MostReadSubElement(
    @SerializedName("basic") val basic: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("text") val text: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("additional_properties") val addProps: MRSEAdditionalProps?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("style") val style: String?,
)

data class MRSEAdditionalProps(
    @SerializedName("resizeUrl") val resizeURL: String?,
)

data class MRELabel(
    @SerializedName("basic") val basic: MostReadSubElement?,
    @SerializedName("transparency") val transparency: MostReadSubElement?,
)

data class MREPromoItems(
    @SerializedName("basic") val basic: MostReadSubElement?,
)

data class MRECredits(
    @SerializedName("by") val by: List<MostReadSubElement>?,
)
