/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.Renderer
import com.wapo.flagship.features.articles2.models.deserialized.*
import com.wapo.flagship.features.articles2.typeconverters.MoshiAdapters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MoshiAdapterModule {

    @Provides
    @Singleton
    fun provideMoshi(): MoshiAdapters {
        return MoshiAdapters(getMoshiBuilder().build())
    }

    /**
     * Returns the moshi instance.
     * IMPORTANT: Please note that the sequence of adding the adapter/adaptor factory matters when building the moshi instance.
     */
    companion object {
        fun getMoshiBuilder(): Moshi.Builder {
            return Moshi.Builder()
                .add(DefaultOnDataMismatchAdapter.newFactory(Item::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Kicker::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Title::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(ByLine::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Date::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Deck::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Image::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Correction::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(SanitizedHtml::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(ListItem::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(PullQuote::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(InterstitialLink::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Link::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(AuthorInfo::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(ElementGroup::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Podcast::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Divider::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Comments::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Anchor::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(Audio::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(OlympicsMedals::class.java, null))
                .add(DefaultOnDataMismatchAdapter.newFactory(TableItem::class.java, null))
                .add(
                    PolymorphicJsonAdapterFactory.of(Item::class.java, "type")
                        .withSubtype(Kicker::class.java, "kicker")
                        .withSubtype(Title::class.java, "title")
                        .withSubtype(ByLine::class.java, "byline")
                        .withSubtype(Date::class.java, "date")
                        .withSubtype(Deck::class.java, "deck")
                        .withSubtype(Image::class.java, "image")
                        .withSubtype(Correction::class.java, "correction")
                        .withSubtype(SanitizedHtml::class.java, "sanitized_html")
                        .withSubtype(ListItem::class.java, "list")
                        .withSubtype(PullQuote::class.java, "pull_quote")
                        .withSubtype(InterstitialLink::class.java, "interstitial_link")
                        .withSubtype(Link::class.java, "link")
                        .withSubtype(AuthorInfo::class.java, "author_info")
                        .withSubtype(ElementGroup::class.java, "element_group")
                        .withSubtype(Podcast::class.java, "podcast")
                        .withSubtype(Divider::class.java, "divider")
                        .withSubtype(Audio::class.java, "audio")
                        .withSubtype(TableItem::class.java, "table")
                        .withSubtype(OlympicsMedals::class.java, "olympics")
                        .withDefaultValue(Item("default"))
                ).add(
                    PolymorphicJsonAdapterFactory.of(Link::class.java, "subtype")
                        .withSubtype(Comments::class.java, "comments")
                        .withSubtype(Anchor::class.java, "anchor")
                        .withDefaultValue(Link("default", "link"))
                ).add(
                    Renderer::class.java, EnumJsonAdapter.create(Renderer::class.java)
                        .withUnknownFallback(Renderer.DEFAULT)
                )
        }
    }

}