package com.wapo.flagship.features.articles2.typeconverters

import com.wapo.android.commons.util.Logger
import androidx.room.TypeConverter
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.squareup.moshi.Moshi
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.models.FtsCarousel
import com.wapo.flagship.features.articles2.models.FtsCarouselJsonAdapter
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.LiveOutcome
import com.wapo.flagship.features.articles2.models.LiveOutcomeJsonAdapter
import com.wapo.flagship.features.articles2.models.SubNav
import com.wapo.flagship.features.articles2.models.SubNavJsonAdapter
import com.wapo.flagship.features.articles2.models.deserialized.*
import com.wapo.flagship.features.articles2.models.deserialized.gallery.Gallery
import com.wapo.flagship.features.articles2.models.deserialized.gallery.GalleryJsonAdapter
import com.wapo.flagship.features.articles2.models.deserialized.instagram.Instagram
import com.wapo.flagship.features.articles2.models.deserialized.instagram.InstagramJsonAdapter
import com.wapo.flagship.features.articles2.models.deserialized.podcast.Podcast
import com.wapo.flagship.features.articles2.models.deserialized.podcast.PodcastJsonAdapter
import com.wapo.flagship.features.articles2.models.deserialized.tweet.Tweet
import com.wapo.flagship.features.articles2.models.deserialized.tweet.TweetJsonAdapter
import com.wapo.flagship.features.articles2.models.deserialized.video.Video
import com.wapo.flagship.features.articles2.models.deserialized.video.VideoJsonAdapter
import org.json.JSONObject

/**
 * [TypeConverter] for list of [Item] objects to be able to serialize and deserialize it
 */
open class ItemListTypeConverter {
    @TypeConverter
    fun toJson(items: List<Item>?): String? {
        if (items == null) {
            return null
        }

        val arr = JsonArray()
        items.forEach {
            when (it) {
                is SubNav -> {
                    arr.add(MoshiAdapters.INSTANCE.subNavAdapter.toJson(it))
                }
                is Pin -> {
                    arr.add(MoshiAdapters.INSTANCE.pinAdapter.toJson(it))
                }
                is Kicker -> {
                    arr.add(MoshiAdapters.INSTANCE.kickerAdapter.toJson(it))
                }
                is Title -> {
                    arr.add(MoshiAdapters.INSTANCE.titleAdapter.toJson(it))
                }
                is ByLine -> {
                    arr.add(MoshiAdapters.INSTANCE.bylineAdapter.toJson(it))
                }
                is ElevatedByline -> {
                    arr.add(MoshiAdapters.INSTANCE.elevatedBylineAdapter.toJson(it))
                }
                is Date -> {
                    arr.add(MoshiAdapters.INSTANCE.dateAdapter.toJson(it))
                }
                is Deck -> {
                    arr.add(MoshiAdapters.INSTANCE.deckAdapter.toJson(it))
                }
                is Image -> {
                    arr.add(MoshiAdapters.INSTANCE.imageAdapter.toJson(it))
                }
                is Correction -> {
                    arr.add(MoshiAdapters.INSTANCE.correctionAdapter.toJson(it))
                }
                is SanitizedHtml -> {
                    arr.add(MoshiAdapters.INSTANCE.sanitizedHtmlAdapter.toJson(it))
                }
                is ListItem -> {
                    arr.add(MoshiAdapters.INSTANCE.listItemAdapter.toJson(it))
                }
                is Video -> {
                    arr.add(MoshiAdapters.INSTANCE.videoAdapter.toJson(it))
                }
                is PullQuote -> {
                    arr.add(MoshiAdapters.INSTANCE.pullQuoteAdapter.toJson(it))
                }
                is BlockQuote -> {
                    arr.add(MoshiAdapters.INSTANCE.blockQuoteAdapter.toJson(it))
                }
                is Quote -> {
                    arr.add(MoshiAdapters.INSTANCE.quoteAdapter.toJson(it))
                }
                is Tweet -> {
                    arr.add(MoshiAdapters.INSTANCE.tweetAdapter.toJson(it))
                }
                is Gallery -> {
                    arr.add(MoshiAdapters.INSTANCE.galleryAdapter.toJson(it))
                }
                is InterstitialLink -> {
                    arr.add(MoshiAdapters.INSTANCE.interstitialLinkAdapter.toJson(it))
                }
                is AuthorInfo -> {
                    arr.add(MoshiAdapters.INSTANCE.authorInfoAdapter.toJson(it))
                }
                is Instagram -> {
                    arr.add(MoshiAdapters.INSTANCE.instagramJsonAdapter.toJson(it))
                }
                is TableItem -> {
                    arr.add(MoshiAdapters.INSTANCE.tableJsonAdapter.toJson(it))
                }
                is Link -> {
                    when (it) {
                        is Comments -> arr.add(MoshiAdapters.INSTANCE.commentsAdapter.toJson(it))
                        is Anchor -> arr.add(MoshiAdapters.INSTANCE.anchorAdapter.toJson(it))
                        is LinkButton ->
                            arr.add(
                                MoshiAdapters.INSTANCE.linkButtonAdapter.toJson(it),
                            )
                        else -> arr.add(MoshiAdapters.INSTANCE.linkAdapter.toJson(it))
                    }
                }
                is Divider -> {
                    arr.add(MoshiAdapters.INSTANCE.dividerAdapter.toJson(it))
                }
                is Podcast -> {
                    arr.add(MoshiAdapters.INSTANCE.podcastJsonAdapter.toJson(it))
                }
                is ElementGroup -> {
                    when (it) {
                        is ElementGroupLinkBox ->
                            arr.add(MoshiAdapters.INSTANCE.elementGroupLinkBoxAdapter.toJson(it))
                        is ElementGroupBlockQuote ->
                            arr.add(MoshiAdapters.INSTANCE.elementGroupBlockQuoteAdapter.toJson(it))
                    }
                }
                is Audio -> {
                    arr.add(MoshiAdapters.INSTANCE.audioJsonAdapter.toJson(it))
                }
                is OlympicsMedals -> {
                    arr.add(MoshiAdapters.INSTANCE.olympicsJsonAdapter.toJson(it))
                }
                is ContextBox -> {
                    arr.add(MoshiAdapters.INSTANCE.contextBoxAdapter.toJson(it))
                }
                is Ad -> {
                    arr.add(MoshiAdapters.INSTANCE.adAdapter.toJson(it))
                }
                is Toggle -> {
                    arr.add(MoshiAdapters.INSTANCE.toggleAdapter.toJson(it))
                }
                is ExpandCollapseCard -> {
                    arr.add(MoshiAdapters.INSTANCE.expandCollapseCardAdapter.toJson(it))
                }
                is InlineCarousel -> {
                    arr.add(MoshiAdapters.INSTANCE.inlineCarouselAdapter.toJson(it))
                }
                is LiveOutcome -> {
                    arr.add(MoshiAdapters.INSTANCE.liveOutcomeAdapter.toJson(it))
                }
                is FtsCarousel -> {
                    arr.add(MoshiAdapters.INSTANCE.ftsCarouselAdapter.toJson(it))
                }
                is AutoRecircCarousel -> {
                    arr.add(MoshiAdapters.INSTANCE.autoRecircCarouselAdapter.toJson(it))
                }
                is WebEmbed -> {
                    arr.add(MoshiAdapters.INSTANCE.webEmbedAdapter.toJson(it))
                }
                is Pdf -> {
                    arr.add(MoshiAdapters.INSTANCE.pdfAdapter.toJson(it))
                }
            }
        }
        return arr.toString()
    }

    @TypeConverter
    fun fromJson(data: String?): List<Item>? {
        if (data == null) {
            return null
        }
        val items = mutableListOf<Item>()
        val jsonArray = JsonParser.parseString(data) as JsonArray
        jsonArray.forEach {
            try {
                val item =
                    when (JSONObject(it.asString)["type"]) {
                        "sub_nav" -> {
                            MoshiAdapters.INSTANCE.subNavAdapter.fromJson(it.asString)
                        }
                        "pin" -> {
                            MoshiAdapters.INSTANCE.pinAdapter.fromJson(it.asString)
                        }
                        "elevated_byline" -> {
                            MoshiAdapters.INSTANCE.elevatedBylineAdapter.fromJson(it.asString)
                        }
                        "kicker" -> {
                            MoshiAdapters.INSTANCE.kickerAdapter.fromJson(it.asString)
                        }
                        "title" -> {
                            MoshiAdapters.INSTANCE.titleAdapter.fromJson(it.asString)
                        }
                        "byline" -> {
                            MoshiAdapters.INSTANCE.bylineAdapter.fromJson(it.asString)
                        }
                        "date" -> {
                            MoshiAdapters.INSTANCE.dateAdapter.fromJson(it.asString)
                        }
                        "deck" -> {
                            MoshiAdapters.INSTANCE.deckAdapter.fromJson(it.asString)
                        }
                        "image" -> {
                            MoshiAdapters.INSTANCE.imageAdapter.fromJson(it.asString)
                        }
                        "correction" -> {
                            MoshiAdapters.INSTANCE.correctionAdapter.fromJson(it.asString)
                        }
                        "sanitized_html" -> {
                            MoshiAdapters.INSTANCE.sanitizedHtmlAdapter.fromJson(it.asString)
                        }
                        "list" -> {
                            MoshiAdapters.INSTANCE.listItemAdapter.fromJson(it.asString)
                        }
                        "video" -> {
                            MoshiAdapters.INSTANCE.videoAdapter.fromJson(it.asString)
                        }
                        "pull_quote" -> {
                            MoshiAdapters.INSTANCE.pullQuoteAdapter.fromJson(it.asString)
                        }
                        "block_quote" -> {
                            MoshiAdapters.INSTANCE.blockQuoteAdapter.fromJson(it.asString)
                        }
                        "quote" -> {
                            MoshiAdapters.INSTANCE.quoteAdapter.fromJson(it.asString)
                        }
                        "tweet" -> {
                            MoshiAdapters.INSTANCE.tweetAdapter.fromJson(it.asString)
                        }
                        "gallery" -> {
                            MoshiAdapters.INSTANCE.galleryAdapter.fromJson(it.asString)
                        }
                        "interstitial_link" -> {
                            MoshiAdapters.INSTANCE.interstitialLinkAdapter.fromJson(it.asString)
                        }
                        "link" -> {
                            when (JSONObject(it.asString)["subtype"]) {
                                "comments" -> {
                                    MoshiAdapters.INSTANCE.commentsAdapter.fromJson(it.asString)
                                }
                                "anchor" -> {
                                    MoshiAdapters.INSTANCE.anchorAdapter.fromJson(it.asString)
                                }
                                "button" -> {
                                    MoshiAdapters.INSTANCE.linkButtonAdapter.fromJson(it.asString)
                                }
                                else -> MoshiAdapters.INSTANCE.linkAdapter.fromJson(it.asString)
                            }
                        }
                        "author_info" -> {
                            MoshiAdapters.INSTANCE.authorInfoAdapter.fromJson(it.asString)
                        }

                        "element_group" -> {
                            when (JSONObject(it.asString)["subtype"]) {
                                ElementGroup.LINK_BOX_SUBTYPE ->
                                    MoshiAdapters.INSTANCE.elementGroupLinkBoxAdapter.fromJson(it.asString)
                                ElementGroup.BLOCK_QUOTE_SUBTYPE ->
                                    MoshiAdapters.INSTANCE.elementGroupBlockQuoteAdapter.fromJson(it.asString)
                                else ->
                                    MoshiAdapters.INSTANCE.elementGroupLinkBoxAdapter.fromJson(it.asString)
                            }
                        }
                        "divider" -> {
                            MoshiAdapters.INSTANCE.dividerAdapter.fromJson(it.asString)
                        }
                        "podcast" -> {
                            MoshiAdapters.INSTANCE.podcastJsonAdapter.fromJson(it.asString)
                        }
                        "audio" -> {
                            MoshiAdapters.INSTANCE.audioJsonAdapter.fromJson(it.asString)
                        }
                        "olympics" -> {
                            MoshiAdapters.INSTANCE.olympicsJsonAdapter.fromJson(it.asString)
                        }
                        "instagram" -> {
                            MoshiAdapters.INSTANCE.instagramJsonAdapter.fromJson(it.asString)
                        }
                        "table" -> {
                            MoshiAdapters.INSTANCE.tableJsonAdapter.fromJson(it.asString)
                        }
                        "context_box" -> {
                            MoshiAdapters.INSTANCE.contextBoxAdapter.fromJson(it.asString)
                        }
                        "ad" -> {
                            MoshiAdapters.INSTANCE.adAdapter.fromJson(it.asString)
                        }
                        "toggle" -> {
                            MoshiAdapters.INSTANCE.toggleAdapter.fromJson(it.asString)
                        }
                        "expand_card" -> {
                            MoshiAdapters.INSTANCE.expandCollapseCardAdapter.fromJson(it.asString)
                        }
                        "inline_carousel" -> {
                            MoshiAdapters.INSTANCE.inlineCarouselAdapter.fromJson(it.asString)
                        }
                        "live_outcome" -> {
                            MoshiAdapters.INSTANCE.liveOutcomeAdapter.fromJson(it.asString)
                        }
                        FtsCarousel.ITEM_NAME -> {
                            MoshiAdapters.INSTANCE.ftsCarouselAdapter.fromJson(it.asString)
                        }
                        AutoRecircCarousel.ITEM_NAME -> {
                            MoshiAdapters.INSTANCE.autoRecircCarouselAdapter.fromJson(it.asString)
                        }
                        "web_embed" -> {
                            MoshiAdapters.INSTANCE.webEmbedAdapter.fromJson(it.asString)
                        }
                        "pdf" -> {
                            MoshiAdapters.INSTANCE.pdfAdapter.fromJson(it.asString)
                        }
                        else -> {
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("Unexpected data found in feeds item")
                                    setModule(LogModules.ARTICLES)
                                    set("json_element", it)
                                }.run {
                                    RemoteLog.e(FlagshipApplication.getInstance(), build())
                                }
                            Logger.e("ItemListTypeConverter", "Unexpected data found in feeds item: $it")
                            Item("default")
                        }
                    }
                if (item != null) {
                    items.add(item)
                }
            } catch (ex: Exception) {
                // If any exception occurs, we assume that the feeds has a corrupted item in the items list. catch (ex: Exception) {
                items.add(Item("default"))
                EventLog
                    .Builder()
                    .apply {
                        setMessage("An unexpected item found in feeds")
                        setModule(LogModules.ARTICLES)
                        set("json_element", it)
                        setErrorMessage(ex.message)
                    }.run {
                        RemoteLog.e(FlagshipApplication.getInstance(), build())
                    }
                Logger.e("ItemListTypeConverter", "An unexpected item found in feeds: $it")
            }
        }
        return items
    }
}

class MoshiAdapters(
    val moshi: Moshi,
) {
    /**
     * Extra reference so [ItemListTypeConverter] can access moshi statically
     */
    companion object {
        lateinit var INSTANCE: MoshiAdapters
    }

    val subNavAdapter: SubNavJsonAdapter by lazy {
        SubNavJsonAdapter(moshi)
    }

    val pinAdapter: PinJsonAdapter by lazy {
        PinJsonAdapter(moshi)
    }

    val kickerAdapter: KickerJsonAdapter by lazy {
        KickerJsonAdapter(moshi)
    }
    val titleAdapter: TitleJsonAdapter by lazy {
        TitleJsonAdapter(moshi)
    }
    val bylineAdapter: ByLineJsonAdapter by lazy {
        ByLineJsonAdapter(moshi)
    }
    val elevatedBylineAdapter: ElevatedBylineJsonAdapter by lazy {
        ElevatedBylineJsonAdapter(moshi)
    }
    val dateAdapter: DateJsonAdapter by lazy {
        DateJsonAdapter(moshi)
    }
    val deckAdapter: DeckJsonAdapter by lazy {
        DeckJsonAdapter(moshi)
    }
    val imageAdapter: ImageJsonAdapter by lazy {
        ImageJsonAdapter(moshi)
    }
    val correctionAdapter: CorrectionJsonAdapter by lazy {
        CorrectionJsonAdapter(moshi)
    }
    val sanitizedHtmlAdapter: SanitizedHtmlJsonAdapter by lazy {
        SanitizedHtmlJsonAdapter(moshi)
    }
    val listItemAdapter: ListItemJsonAdapter by lazy {
        ListItemJsonAdapter(moshi)
    }
    val videoAdapter: VideoJsonAdapter by lazy {
        VideoJsonAdapter(moshi)
    }
    val pullQuoteAdapter: PullQuoteJsonAdapter by lazy {
        PullQuoteJsonAdapter(moshi)
    }
    val blockQuoteAdapter: BlockQuoteJsonAdapter by lazy {
        BlockQuoteJsonAdapter(moshi)
    }
    val quoteAdapter: QuoteJsonAdapter by lazy {
        QuoteJsonAdapter(moshi)
    }
    val tweetAdapter: TweetJsonAdapter by lazy {
        TweetJsonAdapter(moshi)
    }
    val galleryAdapter: GalleryJsonAdapter by lazy {
        GalleryJsonAdapter(moshi)
    }
    val interstitialLinkAdapter: InterstitialLinkJsonAdapter by lazy {
        InterstitialLinkJsonAdapter(moshi)
    }
    val commentsAdapter: CommentsJsonAdapter by lazy {
        CommentsJsonAdapter(moshi)
    }
    val anchorAdapter: AnchorJsonAdapter by lazy {
        AnchorJsonAdapter(moshi)
    }
    val linkButtonAdapter: LinkButtonJsonAdapter by lazy {
        LinkButtonJsonAdapter(moshi)
    }
    val linkAdapter: LinkJsonAdapter by lazy {
        LinkJsonAdapter(moshi)
    }
    val authorInfoAdapter: AuthorInfoJsonAdapter by lazy {
        AuthorInfoJsonAdapter(moshi)
    }
    val elementGroupLinkBoxAdapter: ElementGroupLinkBoxJsonAdapter by lazy {
        ElementGroupLinkBoxJsonAdapter(moshi)
    }
    val elementGroupBlockQuoteAdapter: ElementGroupBlockQuoteJsonAdapter by lazy {
        ElementGroupBlockQuoteJsonAdapter(moshi)
    }
    val dividerAdapter: DividerJsonAdapter by lazy {
        DividerJsonAdapter(moshi)
    }
    val podcastJsonAdapter: PodcastJsonAdapter by lazy {
        PodcastJsonAdapter(moshi)
    }
    val audioJsonAdapter: AudioJsonAdapter by lazy {
        AudioJsonAdapter(moshi)
    }
    val olympicsJsonAdapter: OlympicsMedalsJsonAdapter by lazy {
        OlympicsMedalsJsonAdapter(moshi)
    }
    val instagramJsonAdapter: InstagramJsonAdapter by lazy {
        InstagramJsonAdapter(moshi)
    }
    val tableJsonAdapter: TableItemJsonAdapter by lazy {
        TableItemJsonAdapter(moshi)
    }
    val contextBoxAdapter: ContextBoxJsonAdapter by lazy {
        ContextBoxJsonAdapter(moshi)
    }
    val adAdapter: AdJsonAdapter by lazy {
        AdJsonAdapter(moshi)
    }
    val toggleAdapter: ToggleJsonAdapter by lazy {
        ToggleJsonAdapter(moshi)
    }
    val expandCollapseCardAdapter: ExpandCollapseCardJsonAdapter by lazy {
        ExpandCollapseCardJsonAdapter(moshi)
    }
    val inlineCarouselAdapter: InlineCarouselJsonAdapter by lazy {
        InlineCarouselJsonAdapter(moshi)
    }
    val liveOutcomeAdapter: LiveOutcomeJsonAdapter by lazy {
        LiveOutcomeJsonAdapter(moshi)
    }
    val ftsCarouselAdapter: FtsCarouselJsonAdapter by lazy {
        FtsCarouselJsonAdapter(moshi)
    }
    val autoRecircCarouselAdapter: AutoRecircCarouselJsonAdapter by lazy {
        AutoRecircCarouselJsonAdapter(moshi)
    }
    val webEmbedAdapter: WebEmbedJsonAdapter by lazy {
        WebEmbedJsonAdapter(moshi)
    }
    val pdfAdapter: PdfJsonAdapter by lazy {
        PdfJsonAdapter(moshi)
    }
}
