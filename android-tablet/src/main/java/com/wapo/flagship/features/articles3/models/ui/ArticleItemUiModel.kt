package com.wapo.flagship.features.articles3.models.ui

import com.wapo.adsinf.models.AdConfig
import com.wapo.flagship.features.articles2.models.QuestionSet
import com.wapo.flagship.features.articles2.models.deserialized.ContextBoxAlignment
import com.wapo.flagship.features.articles2.models.deserialized.Position
import com.wapo.flagship.features.articles3.views.PdfUiStyle
import com.wapo.flagship.features.articles3.views.SubNavUiStyle
import com.wapo.flagship.features.articles3.views.AdUiStyle
import com.wapo.flagship.features.articles3.views.AudioPlaybackState
import com.wapo.flagship.features.articles3.views.AudioUiStyle
import com.wapo.flagship.features.articles3.views.AuthorInfoUiStyle
import com.wapo.flagship.features.articles3.views.BlockQuoteUiStyle
import com.wapo.flagship.features.articles3.views.BylineUiStyle
import com.wapo.flagship.features.articles3.views.CarouselUiStyle
import com.wapo.flagship.features.articles3.views.CommentsUiStyle
import com.wapo.flagship.features.articles3.views.ContextBoxUiStyle
import com.wapo.flagship.features.articles3.views.CorrectionUiStyle
import com.wapo.flagship.features.articles3.views.DateUiStyle
import com.wapo.flagship.features.articles3.views.DeckUiStyle
import com.wapo.flagship.features.articles3.views.DividerUiStyle
import com.wapo.flagship.features.articles3.views.ElevatedBylineUiStyle
import com.wapo.flagship.features.articles3.views.ElementGroupUiStyle
import com.wapo.flagship.features.articles3.views.ExpandCollapseUiStyle
import com.wapo.flagship.features.articles3.views.GalleryUiStyle
import com.wapo.flagship.features.articles3.views.HumanAudioUiStyle
import com.wapo.flagship.features.articles3.views.ImageUiStyle
import com.wapo.flagship.features.articles3.views.InlineAlertToggleUiStyle
import com.wapo.flagship.features.articles3.views.InlineOfferUiStyle
import com.wapo.flagship.features.articles3.views.InterstitialLinkUiStyle
import com.wapo.flagship.features.articles3.views.KickerUiStyle
import com.wapo.flagship.features.articles3.views.LinkButtonUiStyle
import com.wapo.flagship.features.articles3.views.ListType
import com.wapo.flagship.features.articles3.views.ListUiStyle
import com.wapo.flagship.features.articles3.views.LiveOutcomeUiStyle
import com.wapo.flagship.features.articles3.views.PinUiStyle
import com.wapo.flagship.features.articles3.views.PodcastUiStyle
import com.wapo.flagship.features.articles3.views.PullQuoteUiStyle
import com.wapo.flagship.features.articles3.views.SanitizedHtmlUiStyle
import com.wapo.flagship.features.articles3.views.TableUiStyle
import com.wapo.flagship.features.articles3.views.TaglineUiStyle
import com.wapo.flagship.features.articles3.views.TitleUiStyle
import com.wapo.flagship.features.articles3.views.VideoUiStyle
import com.wapo.flagship.features.articles3.views.WebEmbedUiStyle
import com.wapo.flagship.features.comments.model.SourceAnnotation
import com.wapo.flagship.features.articles2.models.ArticleInlineMessage
import com.wapo.flagship.features.articles2.models.DisclaimerInfo
import com.wapo.flagship.features.articles2.models.deserialized.KickerImage
import com.wapo.flagship.features.articles3.views.QuoteUiStyle

sealed class ArticleItemUiModel(open val uiStyle: Any)

// Keep these alphabetical
data class AdUiModel(
    val adConfig: AdConfig,
    val breakpoints: List<String?>?,
    val position: Position?,
    override val uiStyle: AdUiStyle
) : ArticleItemUiModel(uiStyle)

data class AudioUiModel(
    val rawUrl: String,
    val title: String?,
    val durationText: String,
    override val uiStyle: AudioUiStyle = AudioUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle) {
    var statusText = AudioStatusText.LISTEN.value
    var playbackState = AudioPlaybackState.READY

    enum class AudioStatusText(val value: String) {
        LISTEN("Listen"),
        NOW_PLAYING("Now Playing")
    }
}

data class AuthorInfoUiModel(
    val id: String,
    val name: String,
    val bio: String?,
    val expertise: String?,
    val imageUrl: String?,
    val showDivider: Boolean = false,
    override val uiStyle: AuthorInfoUiStyle = AuthorInfoUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class BlockQuoteUiModel(
    val content: String,
    val attribution: String,
    val mime: MimeType?,
    override val uiStyle: BlockQuoteUiStyle = BlockQuoteUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class BylineUiModel(
    val text: String,
    val subtext: String?,
    val imageUrl: String?,
    val bio: String?,
    val authors: List<AuthorInfoUiModel> = emptyList(),
    override val uiStyle: BylineUiStyle = BylineUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class ContextBoxUiModel(
    val headline: String,
    val pages: List<ContextBoxPageUiModel> = emptyList(),
    override val uiStyle: ContextBoxUiStyle = ContextBoxUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle) {
    data class ContextBoxPageUiModel(
        val contentElements: List<ArticleItemUiModel> = emptyList(),
        val alignment: ContextBoxAlignment = ContextBoxAlignment.UNKNOWN
    )
}

data class CorrectionUiModel(
    val correctionType: String?,
    val content: String,
    override val uiStyle: CorrectionUiStyle = CorrectionUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class CarouselUiModel(
    val label: String,
    val items: List<CarouselItemUiModel>,
    override val uiStyle: CarouselUiStyle,
    val requestId: String? = null,
    val recipeId: String? = null,
    val testId: String? = null,
    val category: String? = null,
) : ArticleItemUiModel(uiStyle)


data class CommentsUiModel(
    override val uiStyle: CommentsUiStyle = CommentsUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class DateUiModel(
    val content: Long,
    val recencyThreshold: Long?,
    override val uiStyle: DateUiStyle = DateUiStyle.DEFAULT,
) : ArticleItemUiModel(uiStyle)

data class DeckUiModel(
    val content: String,
    override val uiStyle: DeckUiStyle = DeckUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class DividerUiModel(
    override val uiStyle: DividerUiStyle = DividerUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class ElevatedBylineUiModel(
    val kicker: String,
    val byline: String,
    val authors: List<AuthorInfoUiModel>,
    override val uiStyle: ElevatedBylineUiStyle = ElevatedBylineUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

sealed class ElementGroupUiModel(
    open val contentElements: List<SanitizedHtmlUiModel>,
    override val uiStyle: ElementGroupUiStyle = ElementGroupUiStyle.DEFAULT,
) : ArticleItemUiModel(uiStyle) {
    data class ElementGroupLinkBoxUiModel(
        val title: String?,
        override val contentElements: List<SanitizedHtmlUiModel>,
        val subtype: String?,
        val kicker: String?,
        val subheadline: String?,
        val displayDate: String?,
        val expandCollapseUiModel: ExpandCollapseUiModel?,
        override val uiStyle: ElementGroupUiStyle = ElementGroupUiStyle.LINK_BOX
    ) : ElementGroupUiModel(contentElements, uiStyle)

    data class ElementGroupBlockQuoteUiModel(
        override val contentElements: List<SanitizedHtmlUiModel>,
        val attribution: BlockQuoteAttributionUiModel?,
        override val uiStyle: ElementGroupUiStyle = ElementGroupUiStyle.DEFAULT,
    ) : ElementGroupUiModel(contentElements, uiStyle)
}

data class BlockQuoteAttributionUiModel(
    val content: String?,
    val mime: MimeType?,
)

data class ExpandCollapseUiModel(
    val isExpanded: Boolean,
    val expandedLabel: String,
    val truncatedLabel: String,
    val minItemsCount: Int?,
    val group: String,
    override val uiStyle: ExpandCollapseUiStyle = ExpandCollapseUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class GalleryUiModel(
    val images: List<ImageUiModel>,
    val expandCollapseUiModel: ExpandCollapseUiModel?,
    override val uiStyle: GalleryUiStyle = GalleryUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class HumanAudioUiModel(
    val url: String,
    val label: String,
    val durationText: String?,
    val authorImageUrl: String?,
    override val uiStyle: HumanAudioUiStyle = HumanAudioUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle) {
    var playbackState = AudioPlaybackState.READY
}

data class ImageUiModel(
    val imageUrl: String,
    val darkModeImageUrl: String?,
    val caption: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val isLive: Boolean,
    val refreshRateMs: Long?,
    val widthFactor: WidthFactor?,
    override val uiStyle: ImageUiStyle = ImageUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class PinUiModel(
    val content: String,
    override val uiStyle: PinUiStyle = PinUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class PodcastUiModel(
    val rawUrl: String,
    val seriesName: String,
    val episodeName: String,
    val seriesImageUrl: String? = null,
    val durationText: String? = null,
    val statusText: String? = null,
    override val uiStyle: PodcastUiStyle = PodcastUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle) {
    var playbackState = AudioPlaybackState.READY
}

data class PullQuoteUiModel(
    val content: String,
    val attribution: String,
    val mime: MimeType?,
    override val uiStyle: PullQuoteUiStyle = PullQuoteUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class QuoteUiModel(
    val content: String?,
    val attribution: String?,
    val mime: MimeType?,
    override val uiStyle: QuoteUiStyle = QuoteUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class InlineMessageUiModel(
    val articleInlineMessage: ArticleInlineMessage? = null,
    override val uiStyle: InlineOfferUiStyle = InlineOfferUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle) {
    var isVisible: Boolean = false
}

data class InterstitialLinkUiModel(
    val content: String,
    val url: String,
    override val uiStyle: InterstitialLinkUiStyle = InterstitialLinkUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class KickerUiModel(
    val displayLabel: String,
    val displayTransparency: String?,
    val isLive: Boolean,
    val path: String?,
    val alignment: String?,
    val image: KickerImage?,
    override val uiStyle: KickerUiStyle = KickerUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class LinkButtonUiModel(
    val label: String,
    val url: String,
    val showArrow: Boolean,
    override val uiStyle: LinkButtonUiStyle = LinkButtonUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class ListUiModel(
    val items: List<String>,
    val listType: ListType = ListType.UNORDERED,
    val arcId: String? = null,
    override val uiStyle: ListUiStyle = ListUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class LiveOutcomeUiModel(
    val headline: String,
    val subHeadline: String?,
    val imageUrl: String?,
    override val uiStyle: LiveOutcomeUiStyle = LiveOutcomeUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class InlineAlertToggleUiModel(
    val topicDisplayName: String,
    val topicKey: String,
    val isEnabled: Boolean,
    override val uiStyle: InlineAlertToggleUiStyle = InlineAlertToggleUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class SanitizedHtmlUiModel(
    val content: String,
    val questionSets: List<QuestionSet>?,
    val sourceAnnotations: List<SourceAnnotation>?,
    val oEmbed: String?,
    val arcId: String? = null,
    val subheadLevel: Int? = null,
    val mime: MimeType? = null,
    override val uiStyle: SanitizedHtmlUiStyle = SanitizedHtmlUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle) {
    data class AnnotatedText(
        val url: String,
        val start: Int,
        val end: Int
    )
}

/**
 * One chip in the [SubNavUiModel] strip. Selecting it swaps the panel below the strip
 * to [contentUrl]; a tab with a null [contentUrl] is a label only and is not selectable.
 */
data class SubNavTabUiModel(
    val id: String,
    val label: String,
    val contentUrl: String?,
    val subtype: String? = null,
    val behavior: String? = null,
    val iconName: String? = null,
    val children: List<SubNavTabUiModel> = emptyList(),
) {
    val isDropdown: Boolean get() = children.isNotEmpty()
}

/**
 * Horizontally scrolling nav strip with a content panel beneath it.
 *
 * The article element supplies only [tabsUrl] (`{ "type": "sub_nav", "url": "…" }`); the strip
 * and its chips are fetched from there at render time, so the newsroom can change them without
 * an app release. Each chip carries its own content url, which the panel below the strip loads;
 * until a chip is selected the panel stays empty.
 */
data class SubNavUiModel(
    val tabsUrl: String?,
    override val uiStyle: SubNavUiStyle = SubNavUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class TitleUiModel(
    val text: String,
    override val uiStyle: TitleUiStyle = TitleUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class VideoUiModel(
    val contentUrl: String,
    val id: String,
    val thumbnailUrl: String?,
    val title: String?,
    val durationMs: Long?,
    val isAutoplay: Boolean,
    val isLive: Boolean,
    val isLooping: Boolean,
    val hasPromo: Boolean = false,
    val caption: String?,
    val aspectRatio: Float = 16f / 9f,
    override val uiStyle: VideoUiStyle = VideoUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle) {
    /** Whether this video is eligible for automatic playback when scrolled into view. */
    val isAutoplayEligible: Boolean
        get() = isAutoplay || hasPromo
}

data class TableUiModel(
    val header: List<String> = emptyList(),
    val rows: List<List<String>>,
    val hasColumnHeaders: Boolean = false,
    override val uiStyle: TableUiStyle = TableUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class TaglineUiModel(
    override val uiStyle: TaglineUiStyle = TaglineUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle) {
    val tagline = "Democracy Dies in Darkness"
}

data class WebEmbedUiModel(
    val url: String?,
    val oembed: String?,
    val subtype: String?,
    val widthFactor: WidthFactor?,
    override val uiStyle: WebEmbedUiStyle = WebEmbedUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

data class PdfUiModel(
    val url: String,
    override val uiStyle: PdfUiStyle = PdfUiStyle.DEFAULT
) : ArticleItemUiModel(uiStyle)

enum class MimeType(
    val value: String,
) {
    HTML("text/html"),
    PLAIN("text/plain")
}
enum class WidthFactor(val value: String) {
    DEFAULT("default"),
    FULL_BLEED("full-bleed")
}