package com.wapo.flagship.features.wpvideos.data

import com.google.gson.annotations.SerializedName
import com.wapo.flagship.features.grid.model.Item

class WatchVideosApi(
    @SerializedName("items")
    var items: List<WpVideosItem?>? = null
) : Item()

data class WpVideosItem(
    @SerializedName("aspect_ratio")
    var aspectRatio: Double? = null,
    @SerializedName("content_id")
    var contentId: String? = null,
    @SerializedName("promo_image")
    var promoImage: PromoImage? = null,
    @SerializedName("duration")
    var duration: Double? = null,
    @SerializedName("canonical_url")
    var canonicalUrl: String? = null,
    @SerializedName("streams")
    val streams: List<VideoStream?>? = null,
    @SerializedName("item_type")
    var itemType: Int? = 1,
    @SerializedName("adconfig")
    val adConfig: AdConfig? = null,
    @SerializedName("tracking")
    val tracking: Tracking? = null,
    @SerializedName("related_content")
    val relatedContent: List<RelatedContent>? = null,
    @SerializedName("comments")
    val comments: Comments? = null,
) : Item()

data class PromoImage(
    @SerializedName("url")
    val url: String? = null,
)

data class AdConfig(
    @SerializedName("ad_set_url")
    val adSetUrl: String? = null,
    @SerializedName("play_video_ads")
    val playVideoAds: Boolean = true
)

data class Comments(
    @SerializedName("count")
    val count: Int? = null,
)

data class Tracking(
    @SerializedName("page_name")
    val pageName: String? = null,
    @SerializedName("video_category")
    val videoCategory: String? = null,
    @SerializedName("video_section")
    val videoSection: String? = null,
    @SerializedName("video_source")
    val videoSource: String? = null,
    @SerializedName("page_title")
    val pageTitle: String? = null,
    @SerializedName("av_name")
    val avName: String? = null,
    @SerializedName("av_arc_id")
    val avArcId: String? = null,
)

data class RelatedContent(
    @SerializedName("link")
    val link: Link? = null,
)

data class Link(
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("last_modified")
    val lastModified: String? = null,
)

data class VideoStream(
    @SerializedName("bitrate")
    val bitRate: Int? = null,
    @SerializedName("filesize")
    val fileSize: Int? = null,
    @SerializedName("height")
    val height: Int? = null,
    @SerializedName("width")
    val width: Int? = null,
    @SerializedName("provider")
    val provider: String? = null,
    @SerializedName("stream_type")
    val streamType: String? = null,
    @SerializedName("url")
    val url: String? = null
)