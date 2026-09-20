package com.washingtonpost.foryou.data


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import java.util.*

@JsonClass(generateAdapter = true)
data class Basic(
    @Json(name = "aspect_ratio")
    val aspectRatio: String?,
    @Json(name = "credits_display")
    val creditsDisplay: String?,
    @Json(name = "width")
    val width: String?,
    @Json(name = "additional_properties")
    val additionalProperties: AdditionalProperties?,
    @Json(name = "credits_caption_display")
    val creditsCaptionDisplay: String?,
    @Json(name = "url")
    val url: String?,
    @Json(name = "height")
    val height: String?,
    @Json(name = "text")
    val text: String?,
    @Json(name = "style")
    val style: String?,
    @Json(name = "headline_prefix")
    val headlinePrefix: String?
)

@JsonClass(generateAdapter = true)
data class RecommendationsItem(
    @Json(name = "normalized_url")
    val normalizedUrl: String?,
    @Json(name = "label_display")
    val labelDisplay: LabelDisplay?,
    @Json(name = "description")
    val description: Description?,
    @Json(name = "section")
    val section: String?,
    @Json(name = "source")
    val source: String?,
    @Json(name = "type")
    val type: String?,
    @Json(name = "pinned")
    val pinned: Boolean?,
    @Json(name = "article_id")
    val articleId: String?,
    @Json(name = "credits")
    val credits: Credits?,
    @Json(name = "first_publish_date")
    val firstPublishDate: Date?,
    @Json(name = "rank")
    val rank: Int? = 0,
    @Json(name = "additional_properties")
    val additionalProperties: AdditionalProperties?,
    @Json(name = "last_updated_date")
    val lastUpdatedDate: String?,
    @Json(name = "headline")
    val headline: String?,
    @Json(name = "image_url")
    val imageUrl: String?,
    @Json(name = "source_type")
    val sourceType: String?,
    @Json(name = "label")
    val label: Label?,
    @Json(name = "canonical_url")
    val canonicalUrl: String?,
    @Json(name = "promo_items")
    val promoItems: PromoItems?,
    @Json(name = "rec_reason")
    val recReason: String?,
    @Json(name = "url")
    val url: String?,
    @Json(name = "days_since_publish")
    val daysSincePublish: Double? = 0.0,
    @Json(name = "headlines")
    val headlines: Headlines?,
    @Json(name = "_id")
    val arcId: String?,
    @Json(name = "page_name")
    val pageName: String?,
    @Json(name = "category")
    val category: String?,
    @Json(name = "publish_date")
    val publishDate: Date?,
    @Json(name = "authors")
    val authors: List<AuthorsItem>?,
    @Json(name = "display_date")
    val displayDate: Date?,
    @Json(name = "summary")
    val summary: Summary?,
    @Json(name = "comments")
    val comments: Comments?,
    @Json(name = "content_type")
    val contentType: String? = null,
    @Json(name = "video")
    val video: WpVideosItem? = null
)

@JsonClass(generateAdapter = true)
data class Summary(
    @Json(name = "url")
    val url: String?,
    @Json(name = "model_id")
    val modelId: String?,
    @Json(name = "headline")
    val headline: String? = null,
    @Json(name = "summary")
    val summary: String? = null,
    @Json(name = "key_points_heading")
    val keyPointsHeading: String? = null,
    @Json(name = "key_points")
    val keyPoints: List<String?>? = null,
    @Json(name = "disclaimer")
    val disclaimer: String? = null,
    @Json(name = "reviewed")
    val reviewed: Boolean? = false,
)


@JsonClass(generateAdapter = true)
data class ByItem(
    @Json(name = "image")
    val image: Image?,
    @Json(name = "name")
    val name: String?,
    @Json(name = "_id")
    val Id: String?,
    @Json(name = "additional_properties")
    val additionalProperties: AdditionalProperties?
)

@JsonClass(generateAdapter = true)
data class Transparency(
    @Json(name = "display")
    val display: Boolean? = false,
    @Json(name = "text")
    val text: String?,
    @Json(name = "url")
    val url: String?
)

@JsonClass(generateAdapter = true)
data class Headlines(
    @Json(name = "basic")
    val basic: String?
)

@JsonClass(generateAdapter = true)
data class Label(
    @Json(name = "transparency")
    val transparency: Transparency?,
    @Json(name = "basic")
    val basic: Basic?
)

@JsonClass(generateAdapter = true)
data class AuthorsItem(
    @Json(name = "name")
    val name: String?,
    @Json(name = "url")
    val url: String?
)

fun List<AuthorsItem>?.toNamesString(): String? {
    val names = this
        ?.mapNotNull { it.name?.takeIf(String::isNotBlank) }
        ?.takeIf { it.isNotEmpty() }
    return names?.joinToString(", ")}

@JsonClass(generateAdapter = true)
data class PromoItems(
    @Json(name = "basic")
    val basic: Basic?
)

@JsonClass(generateAdapter = true)
data class Image(
    @Json(name = "version")
    val version: String?,
    @Json(name = "url")
    val url: String?
)

@JsonClass(generateAdapter = true)
data class LabelDisplay(
    @Json(name = "transparency")
    val transparency: Transparency?,
    @Json(name = "basic")
    val basic: Basic?
)

@JsonClass(generateAdapter = true)
data class Credits(
    @Json(name = "by")
    val by: List<ByItem>?
)

@JsonClass(generateAdapter = true)
data class ForYouResponse(
    @Json(name = "request_id")
    val requestId: String?,
    @Json(name = "recipe_id")
    val recipeId: String?,
    @Json(name = "test_id")
    val testId: String?,
    @Json(name = "recommendations")
    val recommendations: List<RecommendationsItem>?
)

@JsonClass(generateAdapter = true)
data class Comments (
    @Json(name = "count")
    val count: Int?,
    @Json(name = "url")
    val url: String?
)

@JsonClass(generateAdapter = true)
data class AudioArticle(
    @Json(name = "enabled")
    val enabled: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class AdditionalProperties(
    @Json(name = "size_normalized_url")
    val sizeNormalizedUrl: String?,
    @Json(name = "original")
    val original: Original?,
    @Json(name = "audio_article")
    val audioArticle: AudioArticle?
)

@JsonClass(generateAdapter = true)
data class Original(
    @Json(name = "bio_page")
    val bioPage: String?,
    @Json(name = "byline")
    val byline: String?
)

@JsonClass(generateAdapter = true)
data class Description(
    @Json(name = "basic")
    val basic: String?
)

@JsonClass(generateAdapter = true)
data class HabitTilesResponse(
    @Json(name = "status")
    val status: String?,
    @Json(name = "request_id")
    val requestId: String?,
    @Json(name = "tile_count")
    val tileCount: Int?,
    @Json(name = "tiles")
    val tiles: List<Tile?>?,
    @Json(name = "test_group")
    val testGroup: String?,
)

@JsonClass(generateAdapter = true)
data class Tile(
    @Json(name = "score")
    val score: Double?,
    @Json(name = "tile_category")
    val tileCategory: String?,
    @Json(name = "image_url")
    val imageUrl: String?,
    @Json(name = "context_label")
    val contextLabel: String?,
    @Json(name = "context_indicator")
    val contextIndicator: String?,
    @Json(name = "tile_label")
    val tileLabel: String?,
    @Json(name = "tile_link")
    val tileLink: String?,
    @Json(name = "tile_label_behavior")
    val tileLabelBehavior: String?,
    @Json(name = "tile_category_detail")
    val tileCategoryDetail: String?,
    @Json(name = "position")
    val position: Int?,
    @Json(name = "podcast_metadata")
    val persoPodcastMetadata: PersonalizedPodcast?
)

@JsonClass(generateAdapter = true)
data class WpVideosItem(
    @Json(name = "alt_text")
    var altText: String? = null,
    @Json(name = "content_id")
    var contentId: String? = null,
    @Json(name = "aspect_ratio")
    var aspectRatio: Float? = null,
    @Json(name = "promo_image")
    var promoImage: PromoImage? = null,
    @Json(name = "duration")
    var duration: Double? = null,
    @Json(name = "canonical_url")
    var canonicalUrl: String? = null,
    @Json(name = "streams")
    val streams: List<VideoStream?>? = null,
    @Json(name = "item_type")
    var itemType: Int? = 1,
    @Json(name = "tracking")
    val tracking: Tracking? = null,
    @Json(name = "related_content")
    val relatedContent: List<RelatedContent?>? = null,
)

@JsonClass(generateAdapter = true)
data class PromoImage(
    @Json(name = "url")
    val url: String? = null,
)

@JsonClass(generateAdapter = true)
data class Tracking(
    @Json(name = "page_name")
    val pageName: String? = null,
    @Json(name = "video_category")
    val videoCategory: String? = null,
    @Json(name = "video_section")
    val videoSection: String? = null,
    @Json(name = "video_source")
    val videoSource: String? = null,
    @Json(name = "page_title")
    val pageTitle: String? = null,
    @Json(name = "av_name")
    val avName: String? = null,
    @Json(name = "av_arc_id")
    val avArcId: String? = null,
)

data class RelatedContent(
    @Json(name = "link")
    val link: RelatedContentLink? = null,
)

data class RelatedContentLink(
    @Json(name = "url")
    val url: String? = null,
    @Json(name = "last_modified")
    val lastModified: String? = null,
)
@JsonClass(generateAdapter = true)
data class VideoStream(
    @Json(name = "bit_rate")
    val bitRate: Int? = null,
    @Json(name = "file_size")
    val fileSize: Int? = null,
    @Json(name = "height")
    val height: Int? = null,
    @Json(name = "width")
    val width: Int? = null,
    @Json(name = "provider")
    val provider: String? = null,
    @Json(name = "stream_type")
    val streamType: String? = null,
    @Json(name = "url")
    val url: String? = null
)

enum class TileContextIndicator(val value: String) {
    OMITTED("omitted"),
    RED_BLINKING("red_blinking")
}

enum class TileLabelBehavior(val value: String) {
    WRAP("wrap"),
    SCROLLING("scrolling")
}


