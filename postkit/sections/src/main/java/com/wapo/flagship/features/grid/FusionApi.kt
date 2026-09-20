/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid

import com.google.gson.annotations.SerializedName
import com.washingtonpost.android.recirculation.carousel.models.StyleEntity
import java.io.Serializable
import java.util.Date

class GridEntity : Serializable {
    @SerializedName("regions")
    val regions: MutableList<RegionEntity> = mutableListOf()
    @SerializedName("tracking")
    val tracking: Tracking? = null
    @SerializedName("checksum")
    val checksum: String? = null
    @SerializedName("cards")
    val cards: CardEntity? = null
}

data class Tracking(
    @SerializedName("page_name") val pageName: String,
    val platform: String,
    val site: String,
    @SerializedName("page_type") val pageType: String,
    val section: String?,
    val channel: String,
    val subsection: String?,
    val hierarchy: String,
    @SerializedName("content_type") var contentType: String,
    @SerializedName("story_type") val storyType: String,
    val headline: String,
    val author: String,
    val source: String,
    @SerializedName("content_id") val contentID: String,
    @SerializedName("page_num") val pageNum: String,
    @SerializedName("op_ranking") val opRanking: String,
    @SerializedName("columnname") val columnName: String,
    @SerializedName("blogname") val blogName: String,
    val published: String,
    @SerializedName("news_or_commercial") val newsOrCommercial: String,
    @SerializedName("commercial_node") val commercialNode: String,
    @SerializedName("content_category") val contentCategory: String,
    @SerializedName("sectionfront") val sectionFront: String,
    @SerializedName("track_scrolling") val trackScrolling: String,
    @SerializedName("content_topics") val contentTopics: String,
    @SerializedName("page_title") val pageTitle: String,
    @SerializedName("page_path") val pagePath: String
) : Serializable

enum class SignatureDateFormat {
    @SerializedName("month-day") MONTH_DAY,
    @SerializedName("month-day-year") MONTH_DAY_YEAR,
    @SerializedName("extended") EXTENDED,
    @SerializedName("extended-with-minutes") EXTENDED_WITH_MINUTES;
}


class RegionEntity {
    @SerializedName("location")
    var location: String? = null

    @SerializedName("items")
    var items: MutableList<BaseItemEntity?> = mutableListOf()
}

class TableEntity : CellEntity() {

    @SerializedName("id")
    val id: String? = null

    val items: MutableList<ItemEntity?> = mutableListOf()

    @SerializedName("dividers")
    private val dividers: MutableMap<String, DividersEntity> = mutableMapOf()

    fun getDividers(screenSizeLayout: ScreenSizeLayout) : DividersEntity? {
        return when(screenSizeLayout) {
            ScreenSizeLayout.XLARGE -> dividers["xlarge"]
            ScreenSizeLayout.LARGE -> dividers["large"]
            ScreenSizeLayout.MEDIUM -> dividers["medium"]
            ScreenSizeLayout.SMALL -> dividers["small"]
            ScreenSizeLayout.XSMALL -> dividers["xsmall"]
        }
    }

    @SerializedName("label")
    val label: CompoundLabelEntity? = null

    @SerializedName("cta")
    val cta: CompoundLabelEntity? = null
}

class ChainEntity : BaseItemEntity() {

    @SerializedName("id")
    val id: String? = null

    @SerializedName("layout_attributes")
    var layoutAttributes: LayoutAttributesEntity? = null

    @SerializedName("items")
    val items: MutableList<TableEntity?> = mutableListOf()

    @SerializedName("label")
    val label: CompoundLabelEntity? = null

    @SerializedName("cta")
    val cta: CompoundLabelEntity? = null

    @SerializedName("dividers")
    private val dividers: MutableMap<String, DividersEntity> = mutableMapOf()

    @SerializedName("display_context")
    val displayContext: MutableList<String> = mutableListOf()

    fun getDividers(screenSizeLayout: ScreenSizeLayout) : DividersEntity? {
        return when(screenSizeLayout) {
            ScreenSizeLayout.XLARGE -> dividers["xlarge"]
            ScreenSizeLayout.LARGE -> dividers["large"]
            ScreenSizeLayout.MEDIUM -> dividers["medium"]
            ScreenSizeLayout.SMALL -> dividers["small"]
            ScreenSizeLayout.XSMALL -> dividers["xsmall"]
        }
    }
}

open class ItemEntity : CellEntity() {
    val id: String? = null
    @SerializedName("label") var label: CompoundLabelEntity? = null
    @SerializedName("link_detail") var linkDetail: String? = null
}

abstract class CellEntity : BaseItemEntity() {
    @SerializedName("layout_attributes")
    var layoutAttributes: LayoutAttributesEntity? = null
}

abstract class BaseItemEntity {
    @SerializedName("item_type") var itemType: String? = null
    var bleed: BleedEntity? = BleedEntity.NONE
    @SerializedName("link_group") var linkGroup: String? = null
}

class GridLocationEntity {
    @SerializedName("colspan")
    var span: Int = 0
    var row: Int = 0
    @SerializedName("col")
    var column: Int= 0
    @SerializedName("rowspan")
    var rowSpan: Int = 1
}

class LayoutAttributesEntity {
    @SerializedName("xlarge")
    var extraLarge: GridLocationEntity? = null
    var large: GridLocationEntity? = null
    var medium: GridLocationEntity? = null
    var small: GridLocationEntity? = null
    @SerializedName("xsmall")
    var extraSmall: GridLocationEntity? = null
}

class DividersEntity {
    @SerializedName("vertical")
    val vertical: MutableList<DividerLayoutEntity> = mutableListOf()

    @SerializedName("horizontal")
    val horizontal: MutableList<DividerLayoutEntity> = mutableListOf()
}

class DividerLayoutEntity {
    @SerializedName("col")
    var column = -1
    var row = -1
    @SerializedName("colspan")
    var span = -1
    @SerializedName("rowspan")
    var rowSpan = -1
    @SerializedName("style")
    var style: DividerStyle? = null
}

class SeparatorEntity : BaseItemEntity() {
    @SerializedName("layout_attributes")
    var layoutAttributes: LayoutAttributesEntity? = null
    var size: SeparatorSizeEntity? = null
    var line: Boolean = false
}

enum class SeparatorSizeEntity {
    @SerializedName("x-small", alternate = ["xsmall"])
    XSMALL,
    @SerializedName("small")
    SMALL,
    @SerializedName("large")
    LARGE
}

enum class ScreenSizeLayout {
    XLARGE, LARGE, MEDIUM, SMALL, XSMALL
}

open class HomepageStoryEntity(
    val link: LinkEntity?,
    val media: MediaEntity?,
    val slideshow: SlideShowEntity?,
    open val headline: HeadlineEntity?,
    val source: String?,
    val audio: AudioEntity?,
    @SerializedName("web_embed") val webview: WebComponentEntity?,
    @SerializedName("audio_article") val audioArticle: AudioArticleEntity? = null,
    @SerializedName("offline_link") val offlineLink: LinkEntity?,
    @SerializedName("signature") val signature: SignatureEntity?,
    @SerializedName("blurbs") val blurbs: BlurbsEntity?,
    @SerializedName("text_alignment") val textAlignment: AlignmentEntity?,
    @SerializedName("related_links") val relatedLinks: RelatedLinksEntity? = null,
    @SerializedName("live_ticker") val liveBlog: LiveBlogEntity? = null,
    @SerializedName("wrap_text") val wrapText: Boolean = false,
    val arrangements: ArrangementsEntity?,
    @SerializedName("olympics_medals_table") val olympicsMedals: OlympicsMedalsEntity? = null,
    @SerializedName("olympics_schedule_table") val olympicsSchedule: OlympicsScheduleEntity? = null,
    @SerializedName("cta") var cta: CompoundLabelEntity? = null,
    @SerializedName("footnote") var footNote: FootNoteEntity? = null,
    @SerializedName("actions") val actions: ActionsEntity? = null,
    @SerializedName("topper_label") val topperLabel: CompoundLabelEntity? = null,
    @SerializedName("count") val count: CountEntity? = null,
    @SerializedName("content_id") val contentId: String? = null
) : ItemEntity(), Serializable

abstract class BarEntity : BaseItemEntity(), Serializable {
    val headline: HeadlineEntity? = null
    @SerializedName("link") val link: LinkEntity? = null
    @SerializedName("label") var label: BreakingNewsLabel? = null
    @SerializedName("timestamp") val timestamp: Date? = null

    /**
     * In milliseconds
     */
    fun getTimestampMs() = timestamp?.time ?: 0

    fun getText(): CharSequence? {
        return arrayOf(
                label?.text,
                headline?.text ?: "")
                .joinToString(separator = " ")
    }
}
data class SlideShowEntity(
    @SerializedName("aspect_ratio") val aspectRatio: Float = 1.5f,
    @SerializedName("scaling_strategy") val scalingStrategy: ScalingStrategyType?,
    val link: LinkEntity?,
    val overlay: OverlayEntity?,
    val images: MutableList<SlideShowImagesEntity?>?,
    val bleed: BleedEntity? = BleedEntity.NONE
) : Serializable

class SlideShowImagesEntity(
    val url:String?,
    val caption:String?
)
class BreakingNewsBarEntity : BarEntity()

class LiveVideoBarEntity : BarEntity() {
    @SerializedName("media") val media: MediaEntity? = null
}

class BreakingNewsLabel(
        @SerializedName("text") var text: String? = null
) : Serializable

data class CompoundLabelEntity(
     @SerializedName("type") val type: CompoundLabelTypeEntity? = null,
     @SerializedName("position") val position: LabelPositionEntity? = LabelPositionEntity.DEFAULT,
     @SerializedName("alignment") val alignment: AlignmentEntity? = null,
     @SerializedName("show_arrow") val showArrow: Boolean? = false,
     @SerializedName("icon") val icon: LabelIcon? = null,
     @SerializedName("link") val link: LinkEntity? = null,
     @SerializedName("label") val label: LabelEntity? = null,
     @SerializedName("style") val style: LabelStyleEntity? = null,
     @SerializedName("label_secondary") val labelSecondary: LabelEntity? = null,
     @SerializedName("form") val form: FormEntity? = null
) : Serializable

data class TrackingEntity(
    @SerializedName("feed") val feed:String? = null
)

enum class LabelIcon {
    @SerializedName("camera") CAMERA,
    @SerializedName("chart") CHART,
    @SerializedName("headphones") HEADPHONES,
    @SerializedName("election-star") ELECTION_STAR,
    @SerializedName("play") PLAY,
    @SerializedName("olympics") OLYMPICS,
    @SerializedName("the-7") THE_7,
    @SerializedName("world-cup") WORLD_CUP,
    @SerializedName("postpulse") POST_PULSE,
    @SerializedName("comment") COMMENTS,
    @SerializedName("external-link") EXTERNAL_LINK,
}

enum class LabelStyleEntity {
    @SerializedName("opinions") OPINIONS,
    @SerializedName("wp-intelligence") WP_INTELLIGENCE,
    @SerializedName("the-7-live") THE_SEVEN_LIVE,
}

enum class CompoundLabelTypeEntity {
    @SerializedName("full-span", alternate = ["section-large", "section-large-with-explainer"])
    FULL_SPAN,
    @SerializedName("package", alternate = ["section-small"])
    PACKAGE,
    @SerializedName("pill")
    PILL,
    @SerializedName("mini-all-caps")
    MINI_ALL_CAPS,
    @SerializedName("kicker")
    KICKER,
    @SerializedName("live-updates")
    LIVE_UPDATES,
    @SerializedName("exclusive-pill")
    EXCLUSIVE,
    @SerializedName("package-nested")
    PACKAGE_NESTED,
    @SerializedName("promo")
    PROMO,
    @SerializedName("cta")
    CTA,
    @SerializedName("newsletter")
    NEWSLETTER,
    @SerializedName("button")
    BUTTON,
    @SerializedName("comment")
    COMMENT,
    @SerializedName("brand-promo")
    BRAND_PROMO,
}

enum class LabelPositionEntity {
    @SerializedName("default")
    DEFAULT,
    @SerializedName("above-headline")
    ABOVE_HEADLINE,
}

data class LabelEntity(
    @SerializedName("text") val text: String? = null
) : Serializable

data class LinkEntity(
        val type: LinkTypeEntity,
        val url: String,
        @SerializedName("access_level") val accessLevel: String? = null,
        @SerializedName("last_modified") val lastModified: String? = null,
        @SerializedName("display_date") val displayDate: String? = null,
        val subtype: String? = null
) : Serializable

data class ExcerptEntity(
    val text : String
)
data class MediaEntity(
        @SerializedName("promo_image") val promoImageURL: String?,
        @SerializedName("media_type") val mediaType: MediaTypeEntity?,
        val width: Int,
        val height: Int,
        @SerializedName("aspect_ratio") val aspectRatio: Float = 1.5f,
        val overlay: OverlayEntity?,
        val caption: String?,
        val url: String?,
        val video: VideoEntity?,
        @SerializedName("live_image") val liveImage: LiveImageEntity?,
        val link: LinkEntity?,
        @SerializedName("dynamic_replacement") val dynamicReplacement: SubItemTypeEntity?,
        @SerializedName("alt_text") val altText: String?,
        @SerializedName("make_it_round") val makeItRound: Boolean?,
        val bleed: BleedEntity? = BleedEntity.NONE
) : Serializable

enum class MediaTypeEntity {
    @SerializedName("wp-video")
    POST_TV,
    @SerializedName("youtube")
    YOUTUBE,
    @SerializedName("image")
    FEATURED,
    @SerializedName("live-image")
    LIVE_IMAGE
}

data class WebComponentEntity(
    @SerializedName("url")
    val url: String?,

    @SerializedName("sizes")
    val sizes:List<ComponentSize>?
) : Serializable

data class ComponentSize(
    val width:Int?,
    val height:Int?
) : Serializable

data class VideoEntity(
    @SerializedName("youtube_id")
        val youTubeId: String?,
    @SerializedName("streams")
        val streams: List<StreamEntity>,
    @SerializedName("is_live")
        val isLive: Boolean = false,
    @SerializedName("is_looping")
        val isLooping: Boolean = false,
    val autoplay: Boolean = false,
    val promo: PromoEntity?,
    @SerializedName(value="omniture", alternate=["tracking"])
        val omniture: VideoTrackingEntity?,
    @SerializedName("related_content")
    val relatedContent: List<RelatedContent>? = null,
    @SerializedName("adconfig") val adConfig: AdConfigEntity?,
    @SerializedName("duration") val duration: Long?
) : Serializable

data class PromoEntity(
    @SerializedName("is_looping")
    val isLooping: Boolean = false,
    val url: String?
) : Serializable

data class AdConfigEntity(
    @SerializedName("commercial_ad_node")
    val commercialAdNode: String?,
    @SerializedName("play_video_ads")
    val playVideoAds: Boolean = false,
    @SerializedName("force_ad")
    val forceAd: Boolean = false,
    @SerializedName("allow_preroll_on_domain")
    val allowPrerollOnDomain: Boolean = false,
    @SerializedName("enable_auto_preview")
    val enableAutoPreview: Boolean = false,
    @SerializedName("play_ads")
    val playAds: Boolean = false,
    @SerializedName("auto_play_preroll")
    val autoPlayPreroll: Boolean = false,
    @SerializedName("enable_ad_insertion")
    val enableAdInsertion: Boolean = false,
    @SerializedName("video_ad_zone")
    val videoAdZone: String?,
    @SerializedName("ad_set_url")
    val adSetUrl: String?,
    @SerializedName("primary_section_id")
    val primarySectionId: String?
) : Serializable


data class VideoTrackingEntity(
        @SerializedName("content_id")
        val contentId: String?,
        @SerializedName("page_name")
        val pageName: String?,
        @SerializedName("video_source")
        val videoSource: String?,
        @SerializedName("video_section")
        val videoSection: String?,
        @SerializedName("video_name")
        val videoName: String?,
        @SerializedName("video_category")
        val videoCategory: String?
) : Serializable

data class RelatedContent(
    @SerializedName("link")
    val link: RelatedContentLink? = null,
)

data class RelatedContentLink(
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("last_modified")
    val lastModified: String? = null,
)

data class StreamEntity(
        @SerializedName("bitrate")
        val bitRate: Int?,
        @SerializedName("filesize")
        val fileSize: Int?,
        val height: Int?,
        val width: Int?,
        val provider: String?,
        @SerializedName("stream_type")
        val streamType: String?,
        val url: String
) : Serializable

data class OverlayEntity (
        val text: String?,
        val style: OverlayStyleEntity?,
        @SerializedName("suffix_icon") val suffixIcon: ArtOverlayIconEntity?,
        @SerializedName("prefix_icon") val prefixIcon: ArtOverlayIconEntity?,
        @SerializedName("prefix_media") val prefixMedia: MediaEntity?,
        @SerializedName("secondary_text") val secondaryText: String?,
        @SerializedName("secondary_style") val secondaryStyle: OverlayStyleEntity?
) : Serializable

enum class ArtOverlayIconEntity {
    @SerializedName("arrow") ARROW,
    @SerializedName("camera") CAMERA,
    @SerializedName("play") PLAY,
    @SerializedName("waveform") WAVEFORM
}

enum class OverlayStyleEntity {
    @SerializedName("default") DEFAULT,
    @SerializedName("secondary") SECONDARY,
    @SerializedName("secondary_live") LIVE,
    @SerializedName("compact") COMPACT
}

enum class ArtPositionEntity {
    @SerializedName("art-above-head") HIGH,
    @SerializedName("art-left") LEFT,
    @SerializedName("art-right") RIGHT,
    @SerializedName("art-left-of-blurb") LEFT_OF_BLURB,
    @SerializedName("art-left-of-head") LEFT_OF_HEADLINE,
    @SerializedName("art-right-of-head") RIGHT_OF_HEADLINE,
    @SerializedName("art-right-of-blurb") RIGHT_OF_BLURB,
    @SerializedName("art-below-head") BELOW_HEADLINE;
}

enum class ArtWidthEntity {
    @SerializedName("x-small") XSMALL,
    @SerializedName("x-large") XLARGE,
    @SerializedName("xx-large") XXLARGE,
    @SerializedName("mini") MINI,
    @SerializedName("tiny") TINY,
    @SerializedName("small") SMALL,
    @SerializedName("medium") MEDIUM,
    @SerializedName("large") LARGE,
    @SerializedName("full-width") FULL_WIDTH
}

data class AudioEntity(
    @SerializedName("media_id") val mediaId: String? = null,
    @SerializedName("stream_url") val streamUrl: String? = null,
    @SerializedName("stream_url_no_ads") val streamUrlNoAds: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("display_date") val displayDate: String? = null,
    @SerializedName("label") val label: CompoundLabelEntity? = null,
    @SerializedName("player_media") val playerMediaEntity: MediaEntity? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("tracking") val tracking: AudioTrackingEntity? = null,
    @SerializedName("subscription_links") val subscriptionLinks: SubscriptionLinksEntity? = null,
    @SerializedName("adconfig") val adConfig: AudioAdConfigEntity? = null,
    @SerializedName("series") val series: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("title_prefix") val titlePrefix: String? = null,
) : Serializable

data class AudioAdConfigEntity(
    @SerializedName("use_vast") val useVAST: Boolean? = null,
    @SerializedName("ad_breaks") val adBreaks: List<AudioAdBreakEntity>? = null,
    @SerializedName("ad_set_url") val adSetUrl: String? = null,
    @SerializedName("primary_section_id") val primarySectionId: String? = null,
) : Serializable

data class AudioAdBreakEntity(
    @SerializedName("type") val type: AudioAdBreakType? = null,
    @SerializedName("max_ads") val maxAds: Int? = null,
    @SerializedName("time") val time: Double? = null,
) : Serializable

enum class AudioAdBreakType {
    @SerializedName("preroll") PREROLL,
    @SerializedName("midroll") MIDROLL,
    @SerializedName("postroll") POSTROLL,
}

data class ImmersionEntity(
    @SerializedName("id") val id: String? = null,
    @SerializedName("commercial_node") val commercialNode: String? = null,
) : Serializable

/**
 * Audio articles in flex and audio carousel items
 */
data class AudioArticleEntity(
    @SerializedName("content_url") val contentUrl: String? = null,
    @SerializedName("display_date") val displayDate: String? = null,
    @SerializedName("label") val label: CompoundLabelEntity? = null,
    @SerializedName("title_prefix") val titlePrefix: String? = null,
    @SerializedName("title_separator") val titleSeparator: String? = null,
    val title: String? = null,
    val voices: List<VoiceEntity>? = null,
    @SerializedName("human_voice") val humanVoice: HumanVoiceEntity? = null,
    @SerializedName("preferred_voice") val preferredVoice: AudioArticleVoiceType? = null,
    @SerializedName("tracking") val tracking: AudioArticleTrackingEntity? = null,
    @SerializedName("player_media") val playerMedia: MediaEntity? = null,
    @SerializedName("inline_player") val inlinePlayer: InlinePlayerEntity? = null,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("adconfig") val adConfig: AudioAdConfigEntity? = null,
) : Serializable

class VoiceEntity(
    @SerializedName("voice_id") val voiceId: String? = null,
    val duration: Long? = null,
    val label: String? = null,
    @SerializedName("raw_url") val rawUrl: String? = null,
    @SerializedName("ads_url") val adsUrl: String? = null,
) : Serializable

class HumanVoiceEntity(
    @SerializedName("source_file_id") val sourceFileId: String? = null,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("raw_url") val rawUrl: String? = null,
    @SerializedName("ads_url") val adsUrl: String? = null
) : Serializable

class AudioArticleTrackingEntity(
    @SerializedName("arc_id") val arcId: String? = null,
    @SerializedName("author_id") val authorId: String? = null,
    @SerializedName("author_name") val authorName: String? = null,
    @SerializedName("author_desk") val authorDesk: String? = null,
    @SerializedName("author_subdesk") val authorSubdesk: String? = null,
    @SerializedName("tracking_tags") val trackingTags: String? = null,
    @SerializedName("commercial_node") val commercialNode: String? = null,
    @SerializedName("content_category") val contentCategory: String? = null,
    @SerializedName("content_type") val contentType: String? = null,
    @SerializedName("page_name") val pageName: String? = null,
    @SerializedName("first_publish_date") val firstPublishDate: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("hierarchy") val hierarchy: String? = null,
    @SerializedName("section") val section: String? = null,
    @SerializedName("subsection") val subsection: String? = null,
    @SerializedName("source") val source: String? = null,
) : Serializable

enum class AudioArticleVoiceType {
    @SerializedName("automated") AUTOMATED,
    @SerializedName("human") HUMAN
}

data class AudioTrackingEntity(
    @SerializedName("series_slug") val seriesSlug: String? = null,
    @SerializedName("audio_name") val audioName: String? = null,
) : Serializable

data class HeadlineEntity(
    val text: String,
    val size: SizeEntity,
    val alignment: AlignmentEntity?,
    @SerializedName("font_style") val fontStyle: FontStyleEntity?,
    val type: BulletTypeEntity? = null,
    @SerializedName("deck") val deck: String?,
    @SerializedName("prefix") val prefix: String?,
    @SerializedName("style") val style: String?,
    @SerializedName("icon") val icon: HeadlineIcon? = null,
) : Serializable

enum class HeadlineIcon {
    @SerializedName("logo") LOGO,
    @SerializedName("ripple") RIPPLE,
}

data class InlinePlayerEntity(
    @SerializedName("listen") val listen: String? = null
)

data class SubscriptionLinksEntity(
    @SerializedName("alexa") val alexa: String? = null,
    @SerializedName("amazon_music") val amazonMusic: String? = null,
    @SerializedName("apple_podcasts") val applePodcasts: String? = null,
    @SerializedName("google_play") val googlePlay: String? = null,
    @SerializedName("iheart_radio") val iheartRadio: String? = null,
    @SerializedName("radio_public") val radioPublic: String? = null,
    @SerializedName("rss") val rss: String? = null,
    @SerializedName("spotify") val spotify: String? = null,
    @SerializedName("stitcher") val stitcher: String? = null,
    @SerializedName("tune_in") val tuneIn: String? = null
) : Serializable

enum class AlignmentEntity {
    @SerializedName("center") CENTER,
    @SerializedName("left") LEFT,
    @SerializedName("right") RIGHT,
    @SerializedName("inherit") INHERIT;
}

enum class FontStyleEntity {
    @SerializedName("highlight-style") HIGHLIGHT_STYLE,
    @SerializedName("normal-style") NORMAL_STYLE,
    @SerializedName("thin-style") THIN_STYLE,
    @SerializedName("regular") REGULAR_STYLE,
    @SerializedName("bold") BOLD_STYLE,
    @SerializedName("italic") ITALIC_STYLE,
    @SerializedName("light") LIGHT_STYLE,
}

enum class BulletTypeEntity {
    @SerializedName("normal")
    NORMAL,
    @SerializedName("bullet")
    BULLET
}

data class SignatureEntity(
        @SerializedName("by_line") val byLine: String?,
        val alignment: AlignmentEntity?,
        val section: String?,
        val timestamp: String?,
        @SerializedName("recency_threshold") val recencyThreshold: Long = 0,
        val ratingCharacter: String?,
        val rating: Int?,
        @SerializedName("date_format") val dateFormat: SignatureDateFormat?
) : Serializable

data class BlurbsEntity(
    val items: List<BlurbItemEntity>?,
    val info: BlurbInfoEntity?,
    val style: String?
) : Serializable

data class BlurbItemEntity(
    val text: String?,
    val type: BulletTypeEntity = BulletTypeEntity.NORMAL,
    val mime: String?
) : Serializable

data class BlurbInfoEntity(
        @SerializedName("size") val size: SizeEntity?,
        @SerializedName("font_style") val fontStyle: BlurbStyleEntity?) : Serializable

enum class SizeEntity {
    @SerializedName("tiny") TINY,
    @SerializedName("x-small") XSMALL,
    @SerializedName("small") SMALL,
    @SerializedName("medium") MEDIUM,
    @SerializedName("standard") STANDARD,
    @SerializedName("large") LARGE,
    @SerializedName("x-large") XLARGE,
    @SerializedName("huge") HUGE,
    @SerializedName("massive") MASSIVE,
    @SerializedName("colossal") COLOSSAL,
    @SerializedName("jumbo") JUMBO,
    @SerializedName("gargantuan") GARGANTUAN,
    @SerializedName("colossal-all-caps") COLOSSAL_ALL_CAPS,
    @SerializedName("jumbo-all-caps") JUMBO_ALL_CAPS,
    @SerializedName("gargantuan-all-caps") GARGANTUAN_ALL_CPS
}

enum class BlurbStyleEntity {
    @SerializedName("normal-style") NORMAL_STYLE,
    @SerializedName("like-article-body") LIKE_ARTICLE_BODY
}

class RelatedLinksEntity(
        @SerializedName("items") val items: List<RelatedLinkItemEntity>? = null,
        @SerializedName("info") val info: RelatedLinksInfoEntity? = null,
        @SerializedName("label") val compoundLabel: CompoundLabelEntity? = null
) : Serializable

class RelatedLinkItemEntity(
        @SerializedName("text") val text: String? = null,
        @SerializedName("url")val link: String? = null,
        @SerializedName("type")val type: LinkTypeEntity? = null
) : Serializable

class RelatedLinksInfoEntity(
        val size: SizeEntity?,
        val position: Position?,
        val arrangement: Arrangement?

) : Serializable {
    enum class Position {
        @SerializedName("bottom") BOTTOM,
        @SerializedName("below-sigline") BELOW_SIGLINE,
    }

    enum class Arrangement {
        @SerializedName("stacked") NORMAL,
        @SerializedName("side-by-side") SIDE_BY_SIDE,
        @SerializedName("side-by-side side-by-side-pipes") SIDE_BY_SIDE_PIPES,
    }
}

class LiveBlogEntity(
        @SerializedName("url") var primeTimeURL: String?,
        @SerializedName("num_to_show") var numberToShow: Int,
        @SerializedName("subtypes") var subtypes: List<String?>?,
        @SerializedName("show_timestamps") var showTimestamps: Boolean,
        @SerializedName("label") val compoundLabel: CompoundLabelEntity? = null
)

enum class LinkTypeEntity {
    @SerializedName("article") ARTICLE,
    @SerializedName("gallery") GALLERY,
    @SerializedName("video") VIDEO,
    @SerializedName("web") WEB,
    @SerializedName("none") NONE,
}

class AdItemEntity(
    val advertisement: AdEntity?,
    @SerializedName("primary_section_id") val primarySectionId: String?,
    @SerializedName("ad_type") val adType: String?,
    @SerializedName("content_type") val contentType: String?
) : ItemEntity()

class AdBaseItemEntity(
    val advertisement: AdEntity?,
    @SerializedName("primary_section_id") val primarySectionId: String?,
    @SerializedName("ad_type") val adType: String?,
    @SerializedName("content_type") val contentType: String?,
    @SerializedName("layout_attributes") var layoutAttributes: LayoutAttributesEntity? = null
) : BaseItemEntity()

class AdEntity(
        @SerializedName("commercial_node") val commercialNode: String?
)

class CarouselItemEntity(
    val items: List<CarouselItem?>?,
    @SerializedName("cta") var cta: CompoundLabelEntity? = null,
    @SerializedName("cardify") var cardify: Boolean? = null
) : ItemEntity()

class CarouselBaseItemEntity(
    val items: List<CarouselItem?>?
) : BaseItemEntity()

class CarouselItem(
    val id: String?,
    val media: MediaEntity?,
    val link: LinkEntity?,
    @SerializedName("excerpt") val excerpt: ExcerptEntity?,
    val headline: HeadlineEntity?,
    val signature: SignatureEntity? = null,
    val label: CompoundLabelEntity?=null,
    val tracking: TrackingEntity?=null,
    @SerializedName("rating")val rating: Rating? = null,
    @SerializedName("recipe_info") val recipeInfo: RecipeInfo? = null,
    @SerializedName("audio") val audio: AudioEntity? = null,
    @SerializedName("immersion") val immersion: ImmersionEntity? = null,
    @SerializedName("audio_article") val audioArticle: AudioArticleEntity? = null,
    @SerializedName("text") val text: String?,
    @SerializedName("author") val author: CommentsAuthor?,
    @SerializedName("reactions") val reactions: Reactions?,
    @SerializedName("replies") val replies: Replies?,
    @SerializedName("item_type") val carouselItemType: String? = null
)

data class RecipeInfo(
    @SerializedName("courses") val courses: List<Course?>?,
    @SerializedName("total_time") val totalTime: Int? = 0
)

data class Rating(
    @SerializedName("count") val count: Int?,
    @SerializedName("max") val max: Double?,
    @SerializedName("type") val type: String?,
    @SerializedName("value") val value: Double?
)

data class Course(
    @SerializedName("description") val description: String?,
    @SerializedName("id") val id: String?
)
class LiveImageEntity(
        val tabs: MutableList<LiveImageTabEntity>,
        val type: LiveImageTypeEntity?,
        val cta: String?
)

enum class LiveImageTypeEntity {
    @SerializedName("tab") TAB,
    @SerializedName("segment") SEGMENT,
    @SerializedName("carousel") CAROUSEL,
}

class LiveImageTabEntity(
        val images: MutableList<LiveImageInfoEntity>,
        val text: String?
)

class LiveImageInfoEntity(
        val url: String,
        @SerializedName("dark_mode_url") val darkModeUrl: String?,
        @SerializedName("aspect_ratio") val aspectRatio: Float = 1.5f,
        @SerializedName("alt_text") val alternateText: String?
)

class InlineOfferEntity : ItemEntity()

enum class ItemType(val serializedName: String) {
    CHAIN("grid"),
    CAROUSEL_CHAIN("grid/brights"),
    SEPARATOR("separator"),
    AD_FLEX_APP("ad/flex-app"),
    AD_BANNER_FLEX_APP("ad-banner/flex-app"),
    AD_IN_TABLE("ad/in-table"),
    HOMEPAGE_STORY("fronts/flex-feature"),
    BREAKING_NEWS_BAR("page/breaking-news-bar"),
    LIVE_VIDEO_BAR("fronts/video-live-bar"),
    VOTE("elections/voting-guide"),
    ELECTIONS_DELAY("elections/delay-message"),
    CAROUSEL("fronts/brights"),
    PROMO("fronts/promo"),
    CAROUSEL_VIDEO("carousel/video"),
    CAROUSEL_AUDIO("carousel/audio"),
    CAROUSEL_AUDIO_PLAYLIST_DEPRECATED("carousel/audio_playlist"),
    CAROUSEL_AUDIO_PLAYLIST("carousel/audio-playlist"),
    CAROUSEL_PERSONALIZED_PODCAST("carousel/personalized_podcast"),
    IMMERSION_CAROUSEL("carousel/immersion"),
    CAROUSEL_RECIPE("carousel/recipe"),
    CAROUSEL_LIVE_IMAGE("carousel/live-image"),
    CAROUSEL_COMMENTS("carousel/comments"),
    CAROUSEL_EXTERNAL("carousel/external"),
    CAROUSEL_SEVEN_LIVE("carousel/7-live"),
    VIDEO("video"),
    HABIT_TILES("fronts/habit-tiles"),
    HABIT_TILES_IN_TABLE("fronts/in-table-habit-tiles"),
    SECTION_TOPPER("fronts/section-topper"),
    INLINE_OFFER("fronts/inline-subs-module");
    override fun toString(): String {
        return serializedName
    }
}

class VoteEntity : ItemEntity(), Serializable

class ElectionsDelayEntity(
        @SerializedName("text") val text: String?,
        @SerializedName("link_text") val linkText: String?,
        @SerializedName("url") val url: String?
) : ItemEntity(), Serializable

class ArrangementsEntity(
        val default: DefaultArrangementEntity?,
        val left: ZoneEntity?,
        val right: ZoneEntity?,
        val top: ZoneEntity?,
        val bottom: ZoneEntity?,
        val sidebar: ZoneEntity?
)

class DefaultArrangementEntity(
        val left: ZoneEntity?,
        val right: ZoneEntity?,
        val top: ZoneEntity?,
        val bottom: ZoneEntity?,
        val sidebar: ZoneEntity?
)
class ZoneEntity(
    val items: MutableList<SubItemTypeEntity?>?,
    val width: ArtWidthEntity?,
    val valign: VerticalAlignmentEntity?
)

enum class SubItemTypeEntity {
    @SerializedName("headline") HEADLINE,
    @SerializedName("media") MEDIA,
    @SerializedName("slideshow") SLIDESHOW,
    @SerializedName("signature") BYLINE,
    @SerializedName("blurbs") BLURB,
    @SerializedName("label") LABEL,
    @SerializedName("live_ticker") LIVE_TICKER,
    @SerializedName("related_links") RELATED_LINKS,
    @SerializedName("audio") AUDIO,
    @SerializedName("audio_article") AUDIO_ARTICLE,
    @SerializedName("olympics_medals_table") OLYMPICS_MEDALS,
    @SerializedName("olympics_schedule_table") OLYMPICS_SCHEDULE,
    @SerializedName("cta") CTA,
    @SerializedName("footnote") FOOT_NOTE,
    @SerializedName("topper_label") TOPPER_LABEL,
    @SerializedName("web_embed") WEB_VIEW,
    @SerializedName("count") COUNT
}

class OlympicsMedalsEntity(
    @SerializedName("title") val title: String?,
    @SerializedName("data") val data: List<OlympicsMedalsEntryEntity?>?,
    @SerializedName("cta") val cta: OlympicsCtaEntity?,
    val bleed: BleedEntity? = BleedEntity.NONE
): Serializable

class OlympicsScheduleEntity(
    @SerializedName("title") val title: String?,
    @SerializedName("data") val data: List<OlympicsScheduleEntryEntity?>?,
    @SerializedName("cta") val cta: OlympicsCtaEntity?,
    val bleed: BleedEntity? = BleedEntity.NONE
): Serializable

class OlympicsMedalsEntryEntity(
    @SerializedName("rank") val rank: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("bronze") val bronze: Int?,
    @SerializedName("silver") val silver: Int?,
    @SerializedName("gold") val gold: Int?,
    @SerializedName("total") val total: Int?,
    val bleed: BleedEntity? = BleedEntity.NONE
): Serializable

class OlympicsScheduleEntryEntity(
    @SerializedName("start") val start: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
): Serializable

class OlympicsCtaEntity(
    @SerializedName("title") val title: String?,
    @SerializedName("link") val link: OlympicsLinkEntity?,
): Serializable

class OlympicsLinkEntity(
    @SerializedName("url") val url: String?,
): Serializable

class FootNoteEntity(
    @SerializedName("text") val text: String?,
    @SerializedName("alignment") val alignment: AlignmentEntity?,
    @SerializedName("mime") val mime: String?,
    @SerializedName("link") val link: LinkEntity?,
): Serializable

enum class ScalingStrategyType {
    @SerializedName("aspect-fit")
    FIT,
    @SerializedName("aspect-fill")
    FILL
}

enum class DividerStyle {
    @SerializedName("normal")
    NORMAL,
    @SerializedName("bold")
    BOLD
}

enum class BleedEntity {
    @SerializedName("full")
    FULL,
    @SerializedName("container")
    CONTAINER,
    @SerializedName("none")
    NONE
}

class CardEntity(
    @SerializedName("xsmall")
    val extraSmall: CardLayoutEntity?
)

class CardLayoutEntity(
    @SerializedName("grids")
    val grids: List<List<String>>?,
    @SerializedName("tables")
    val tables: List<List<String>>?,
    @SerializedName("features")
    val features: List<List<String>>?
)

class ActionsEntity(
    @SerializedName("audio_article")
    val audioArticle: AudioArticleEntity?,
    @SerializedName("comments")
    val comments: CommentsActionEntity?,
)

data class CommentsActionEntity(
    @SerializedName("count") val count: Int?,
    @SerializedName("url") val url: String?,
) : Serializable

enum class VerticalAlignmentEntity {
    @SerializedName("center")
    CENTER,
    @SerializedName("bottom")
    BOTTOM,
}

data class FormEntity(
    @SerializedName("action")
    val action: String?,
    @SerializedName("fields")
    val fields: List<FormFieldEntity?>?,
)

data class FormFieldEntity(
    @SerializedName("type")
    val type: String?,
    @SerializedName("param")
    val param: String?,
    @SerializedName("placeholder")
    val placeholder: String?,
)

class HabitTilesEntity(
    @SerializedName("cta") var cta: CompoundLabelEntity? = null
) : ItemEntity()

class CountEntity(
    @SerializedName("count") var count: String?,
    @SerializedName("size") var size: SizeEntity?,
)

class CommentsAuthor(
    @SerializedName("name") var name: String?,
    @SerializedName("role") var role: String?,
    @SerializedName("avatar") var avatar: String?,
)

class Reactions(
    @SerializedName("count") var count: Int?,
)

class Replies(
    @SerializedName("count") var count: Int?,
    @SerializedName("avatars") var avatars: List<String?>?,
)

class SectionTopperEntity(
    @SerializedName("style") var style: SectionTopperStyleTypeEntity?,
    @SerializedName("title") var title: String?,
    @SerializedName("tagline") var tagline: String?,
) : ItemEntity()

enum class SectionTopperStyleTypeEntity {
    @SerializedName("comments") COMMENTS
}
