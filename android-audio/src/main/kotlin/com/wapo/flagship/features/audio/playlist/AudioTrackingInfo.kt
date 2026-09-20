package com.wapo.flagship.features.audio.playlist

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AudioTrackingInfo(
    @Json(name = "page_name") val pageName: String? = null,
    @Json(name = "page_number") val pageNumber: String? = null,
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "content_subsection") val contentSubsection: String? = null,
    @Json(name = "content_type") val contentType: String? = null,
    @Json(name = "content_author") val contentAuthor: String? = null,
    @Json(name = "search_keywords") val searchKeywords: String? = null,
    @Json(name = "page_format") val pageFormat: String? = null,
    @Json(name = "blog_name") val blogName: String? = null,
    @Json(name = "content_source") val contentSource: String? = null,
    @Json(name = "content_url") val contentURL: String? = null,
    @Json(name = "interface_type") val interfaceType: String? = null,
    @Json(name = "content_id") val contentId: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "primary_section") val primarySection: String? = null,
    @Json(name = "secondary_section") val secondarySection: String? = null,
    @Json(name = "sub_section") val subSection: String? = null,
    @Json(name = "arc_id") val arcId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "author_id") val authorId: String? = null,
    @Json(name = "newsroom_desk") val newsroomDesk: String? = null,
    @Json(name = "newsroom_subdesk") val newsroomSubdesk: String? = null,
    @Json(name = "first_published") val firstPublished: Long? = null,
    @Json(name = "content_topics") val contentTopics: String? = null,
    @Json(name = "tracking_tags") val trackingTags: String? = null,
    @Json(name = "commercial_node") val commercialNode: String? = null,
    @Json(name = "content_category") val contentCategory: String? = null,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "hierarchy") val hierarchy: String? = null,
    @Json(name = "audio_first_publish_date") val audioFirstPublishDate: String? = null
)