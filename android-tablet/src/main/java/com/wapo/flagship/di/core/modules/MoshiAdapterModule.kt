package com.wapo.flagship.di.core.modules

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.wapo.flagship.features.articles2.models.ElementGroupItem
import com.wapo.flagship.features.articles2.models.FtsCarousel
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.LiveOutcome
import com.wapo.flagship.features.articles2.models.Renderer
import com.wapo.flagship.features.articles2.models.SubNav
import com.wapo.flagship.features.articles2.models.deserialized.*
import com.wapo.flagship.features.articles2.models.deserialized.gallery.Gallery
import com.wapo.flagship.features.articles2.models.deserialized.instagram.Instagram
import com.wapo.flagship.features.articles2.models.deserialized.podcast.Podcast
import com.wapo.flagship.features.articles2.models.deserialized.tweet.Tweet
import com.wapo.flagship.features.articles2.models.deserialized.video.Video
import com.wapo.flagship.features.articles2.typeconverters.MoshiAdapters
import com.wapo.flagship.features.search2.model.PostAnswerCarousel
import com.wapo.flagship.features.search2.model.PostAnswerCarouselItem
import com.wapo.flagship.features.search2.model.PostAnswerDivider
import com.wapo.flagship.features.search2.model.PostAnswerFeedback
import com.wapo.flagship.features.search2.model.PostAnswerItem
import com.wapo.flagship.features.search2.model.PostAnswerResponse
import com.wapo.flagship.features.search2.model.PostAnswerText
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object MoshiAdapterModule {
    @Singleton
    @Provides
    fun provideMoshi(): MoshiAdapters =
        MoshiAdapters(getMoshiBuilder().build())
            .also { MoshiAdapters.INSTANCE = it }

    /**
     * Returns the moshi instance.
     * IMPORTANT: Please note that the sequence of adding the adapter/adaptor factory matters when building the moshi instance.
     */
    fun getMoshiBuilder(): Moshi.Builder =
        Moshi
            .Builder()
            .add(DefaultOnDataMismatchAdapter.newFactory(Item::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Pin::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Kicker::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Title::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(SubNav::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ElevatedByline::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ByLine::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Date::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Deck::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Image::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Correction::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(SanitizedHtml::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ListItem::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Video::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(PullQuote::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Quote::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Tweet::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Gallery::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(InterstitialLink::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Link::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(AuthorInfo::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ElementGroup::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ElementGroupLinkBox::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ElementGroupBlockQuote::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Podcast::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Divider::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Comments::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Anchor::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(LinkButton::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Audio::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(OlympicsMedals::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Instagram::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(TableItem::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ContextBox::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Ad::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(WebEmbed::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Pdf::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Toggle::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ExpandCollapseCard::class.java, null))
            .add(
                DefaultOnDataMismatchAdapter.newFactory(GalleryExpandCollapse::class.java, null),
            ).add(DefaultOnDataMismatchAdapter.newFactory(InlineCarousel::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(LiveOutcome::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(FtsCarousel::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(AutoRecircCarousel::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(BlockQuote::class.java, null))
            .add(
                PolymorphicJsonAdapterFactory
                    .of(Item::class.java, "type")
                    .withSubtype(Pin::class.java, "pin")
                    .withSubtype(Kicker::class.java, "kicker")
                    .withSubtype(Title::class.java, "title")
                    .withSubtype(ByLine::class.java, "byline")
                    .withSubtype(ElevatedByline::class.java, "elevated_byline")
                    .withSubtype(Date::class.java, "date")
                    .withSubtype(Deck::class.java, "deck")
                    .withSubtype(Image::class.java, "image")
                    .withSubtype(Correction::class.java, "correction")
                    .withSubtype(SanitizedHtml::class.java, "sanitized_html")
                    .withSubtype(WebEmbed::class.java, "web_embed")
                    .withSubtype(Pdf::class.java, "pdf")
                    .withSubtype(ListItem::class.java, "list")
                    .withSubtype(Video::class.java, "video")
                    .withSubtype(PullQuote::class.java, "pull_quote")
                    .withSubtype(Quote::class.java, "quote")
                    .withSubtype(Tweet::class.java, "tweet")
                    .withSubtype(Gallery::class.java, "gallery")
                    .withSubtype(InterstitialLink::class.java, "interstitial_link")
                    .withSubtype(Link::class.java, "link")
                    .withSubtype(AuthorInfo::class.java, "author_info")
                    .withSubtype(ElementGroup::class.java, "element_group")
                    .withSubtype(Podcast::class.java, "podcast")
                    .withSubtype(Divider::class.java, "divider")
                    .withSubtype(Audio::class.java, "audio")
                    .withSubtype(Instagram::class.java, "instagram")
                    .withSubtype(TableItem::class.java, "table")
                    .withSubtype(OlympicsMedals::class.java, "olympics")
                    .withSubtype(ContextBox::class.java, "context_box")
                    .withSubtype(Ad::class.java, "ad")
                    .withSubtype(Toggle::class.java, "toggle")
                    .withSubtype(ExpandCollapseCard::class.java, "expand_card")
                    .withSubtype(GalleryExpandCollapse::class.java, "gallery_expand_card")
                    .withSubtype(InlineCarousel::class.java, "inline_carousel")
                    .withSubtype(LiveOutcome::class.java, "live_outcome")
                    .withSubtype(SubNav::class.java, "sub_nav")
                    .withSubtype(FtsCarousel::class.java, FtsCarousel.ITEM_NAME)
                    .withSubtype(AutoRecircCarousel::class.java, AutoRecircCarousel.ITEM_NAME)
                    .withSubtype(BlockQuote::class.java, "block_quote")
                    .withDefaultValue(Item("default")),
            ).add(
                PolymorphicJsonAdapterFactory
                    .of(Link::class.java, "subtype")
                    .withSubtype(Comments::class.java, "comments")
                    .withSubtype(Anchor::class.java, "anchor")
                    .withSubtype(LinkButton::class.java, "button")
                    .withSubtype(LinkButton::class.java, "button-outcome")
                    .withDefaultValue(Link("default", "link")),
            ).add(
                Renderer::class.java,
                EnumJsonAdapter
                    .create(Renderer::class.java)
                    .withUnknownFallback(Renderer.DEFAULT),
            ).add(
                ContextBoxAlignment::class.java,
                EnumJsonAdapter
                    .create(ContextBoxAlignment::class.java)
                    .withUnknownFallback(ContextBoxAlignment.UNKNOWN)
                    .nullSafe(),
            ).add(
                PolymorphicJsonAdapterFactory
                    .of(ElementGroupItem::class.java, "type")
                    .withSubtype(SanitizedHtml::class.java, "sanitized_html")
                    .withSubtype(ListItem::class.java, "list")
                    .withSubtype(Title::class.java, "title")
                    .withSubtype(Image::class.java, "image")
                    .withDefaultValue(
                        SanitizedHtml(
                            content = null,
                            mime = null,
                            subtype = null,
                            type = null,
                            subheadLevel = null,
                            style = null,
                            arcId = null,
                            oembed = null,
                            truncate = null,
                        ),
                    ),
            ).add(DefaultOnDataMismatchAdapter.newFactory(PostAnswerResponse::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(PostAnswerItem::class.java, null))
            .add(
                PolymorphicJsonAdapterFactory
                    .of(PostAnswerItem::class.java, "type")
                    .withSubtype(PostAnswerText::class.java, "text")
                    .withSubtype(PostAnswerCarousel::class.java, "carousel")
                    .withSubtype(PostAnswerDivider::class.java, "divider")
                    .withSubtype(PostAnswerFeedback::class.java, "feedback"),
            ).add(
                DefaultOnDataMismatchAdapter.newFactory(
                    PostAnswerCarouselItem::class.java,
                    null,
                ),
            ).add(
                PolymorphicJsonAdapterFactory
                    .of(ElementGroup::class.java, "subtype")
                    .withSubtype(ElementGroupLinkBox::class.java, ElementGroup.LINK_BOX_SUBTYPE)
                    .withSubtype(ElementGroupBlockQuote::class.java, ElementGroup.BLOCK_QUOTE_SUBTYPE)
            )
}
