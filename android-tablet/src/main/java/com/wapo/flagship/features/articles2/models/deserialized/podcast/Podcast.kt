// Copyright (c) 2021 The Washington Post. All rights reserved.

package com.wapo.flagship.features.articles2.models.deserialized.podcast

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.ArticleAudioAdConfig
import com.wapo.flagship.features.articles2.models.deserialized.InlinePlayer

@JsonClass(generateAdapter = true)
data class Podcast(
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "inlinePlayer")
    val inlinePlayer: InlinePlayer?,
    @Json(name = "mediaID")
    val mediaId: String,
    @Json(name = "seriesSlug")
    val seriesSlug: String?,
    @Json(name = "episodeSlug")
    val episodeSlug: String?,
    @Json(name = "seriesName")
    val seriesName: String,
    @Json(name = "episodeName")
    val episodeName: String,
    @Json(name = "seriesImageURL")
    val seriesImageUrl: String?,
    @Json(name = "podtracURL")
    val podtracUrl: String?,
    @Json(name = "duration")
    val duration: Long,
    @Json(name = "subscriptionLinks")
    val subscriptionLinks: SubscriptionLinks?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "completeUrl")
    val completeUrl: String? = null,
    @Json(name = "adconfig")
    val adConfig: ArticleAudioAdConfig? = null,
) : Item(type) {
    enum class SubType(val value: String) {
        INLINE("inline")
    }
}
