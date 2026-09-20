/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Podcast(
    @Json(name = "subtype")
    val subtype: String?,
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
    val seriesImageUrl: String,
    @Json(name = "podtracURL")
    val podtracUrl: String,
    @Json(name = "duration")
    val duration: Long,
    @Json(name = "type")
    override val type: String?
) : Item(type)