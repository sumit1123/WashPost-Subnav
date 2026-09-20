/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OmnitureX(
    @Json(name = "arcId")
    val arcId: String? = null,
    @Json(name = "authorId")
    val authorId: String? = null,
    @Json(name = "authorType")
    val authorType: String? = null,
    @Json(name = "channel")
    val channel: String? = null,
    @Json(name = "contentAuthor")
    val contentAuthor: String? = null,
    @Json(name = "contentId")
    val contentId: String? = null,
    @Json(name = "contentSource")
    val contentSource: String? = null,
    @Json(name = "contentTopics")
    val contentTopics: String? = null,
    @Json(name = "contentType")
    val contentType: String? = null,
    @Json(name = "newsroomDesk")
    val newsroomDesk: String? = null,
    @Json(name = "newsroomSubdesk")
    val newsroomSubdesk: String? = null,
    @Json(name = "pageName")
    val pageName: String? = null,
    @Json(name = "subSection")
    val subSection: String? = null,
    @Json(name = "trackingTags")
    val trackingTags: String? = null,
    @Json(name = "title")
    val title: String? = null,
)