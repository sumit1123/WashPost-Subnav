// Copyright (c) 2023 The Washington Post. All rights reserved.
package com.wapo.flagship.features.articles2.utils

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.AD_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.ANCHOR
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.AUTO_RECIRC_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.AUDIO_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.AUTHOR_INFO_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.BLOCK_QUOTE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.BYLINE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.COMMENT_SUB_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.CONTEXT_BOX_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.CORRECTION_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.DATE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.DECK_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.DIVIDER_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.ELEMENT_GROUP_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.ELEVATED_BYLINE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.EXPAND_COLLAPSE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.FOR_YOU_RECIRC_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.FTS_CAROUSEL_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.GALLERY_EXPAND_COLLAPSE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.HUMAN_AUDIO_SUBTYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.HUMAN_AUDIO_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.IMAGE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.INLINE_ALERT_TOGGLE_ITEM
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.INLINE_CAROUSEL_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.INLINE_EXTRA_ACCOUNT_TOGGLE_ITEM
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.INLINE_PODCAST_SUBTYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.INLINE_PODCAST_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.INLINE_TOPIC_FOLLOW
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.INSTAGRAM_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.INTERSTITIAL_LINK_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.KICKER_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.LINK_BUTTON_SUB_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.LIST_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.LIVE_OUTCOME
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.LUF_LIVE_IMAGE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.MOST_READ
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.OEMBED_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.OLYMPICS_MEDALS
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.PDF_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.PIN_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.PODCAST_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.PULL_QUOTE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.QUOTE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.SANITIZED_HTML
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.STANDALONE_AUDIO_SUBTYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.STANDALONE_AUDIO_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.SUB_NAV
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.TABLE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.TAGLINE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.TITLE_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.TWEET_TYPE
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.VIDEO_TYPE
import com.wapo.flagship.features.articles2.models.FtsCarousel
import com.wapo.flagship.features.articles2.models.InlineAlertToggleItem
import com.wapo.flagship.features.articles2.models.InlineMessageItem
import com.wapo.flagship.features.articles2.models.InlineTopicFollowItem
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.LiveOutcome
import com.wapo.flagship.features.articles2.models.SubNav
import com.wapo.flagship.features.articles2.models.deserialized.Ad
import com.wapo.flagship.features.articles2.models.deserialized.Anchor
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.articles2.models.deserialized.AuthorInfo
import com.wapo.flagship.features.articles2.models.deserialized.AutoRecircCarousel
import com.wapo.flagship.features.articles2.models.deserialized.BlockQuote
import com.wapo.flagship.features.articles2.models.deserialized.ByLine
import com.wapo.flagship.features.articles2.models.deserialized.Comments
import com.wapo.flagship.features.articles2.models.deserialized.ContextBox
import com.wapo.flagship.features.articles2.models.deserialized.Correction
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.articles2.models.deserialized.Deck
import com.wapo.flagship.features.articles2.models.deserialized.Divider
import com.wapo.flagship.features.articles2.models.deserialized.ElementGroup
import com.wapo.flagship.features.articles2.models.deserialized.ElevatedByline
import com.wapo.flagship.features.articles2.models.deserialized.ExpandCollapseCard
import com.wapo.flagship.features.articles2.models.deserialized.ForYouRecirculationItem
import com.wapo.flagship.features.articles2.models.deserialized.GalleryExpandCollapse
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.models.deserialized.InlineCarousel
import com.wapo.flagship.features.articles2.models.deserialized.InterstitialLink
import com.wapo.flagship.features.articles2.models.deserialized.Kicker
import com.wapo.flagship.features.articles2.models.deserialized.LinkButton
import com.wapo.flagship.features.articles2.models.deserialized.ListItem
import com.wapo.flagship.features.articles2.models.deserialized.OlympicsMedals
import com.wapo.flagship.features.articles2.models.deserialized.Pdf
import com.wapo.flagship.features.articles2.models.deserialized.Pin
import com.wapo.flagship.features.articles2.models.deserialized.PullQuote
import com.wapo.flagship.features.articles2.models.deserialized.Quote
import com.wapo.flagship.features.articles2.models.deserialized.RecirculationItem
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.models.deserialized.TableItem
import com.wapo.flagship.features.articles2.models.deserialized.Tagline
import com.wapo.flagship.features.articles2.models.deserialized.Title
import com.wapo.flagship.features.articles2.models.deserialized.WebEmbed
import com.wapo.flagship.features.articles2.models.deserialized.instagram.Instagram
import com.wapo.flagship.features.articles2.models.deserialized.podcast.Podcast
import com.wapo.flagship.features.articles2.models.deserialized.tweet.Tweet
import com.wapo.flagship.features.articles2.models.deserialized.video.Video

/**
 * Helper class for Articles2ItemsRecyclerViewAdapter.kt to get the article item view types.
 */
object ArticleItemAdapterHelper {
    fun getItemViewType(item: Item): Int =
        when (item) {
            is Pin -> PIN_TYPE
            is Kicker -> KICKER_TYPE
            is Title -> TITLE_TYPE
            is ByLine -> BYLINE_TYPE
            is ElevatedByline -> ELEVATED_BYLINE_TYPE
            is Date -> DATE_TYPE
            is Deck -> DECK_TYPE
            is SanitizedHtml -> {
                if (item.subtype == "twitter") {
                    OEMBED_TYPE
                } else {
                    SANITIZED_HTML
                }
            }
            is Image -> if (item.isLive) LUF_LIVE_IMAGE_TYPE else IMAGE_TYPE
            is Correction -> CORRECTION_TYPE
            is ListItem -> LIST_TYPE
            is Divider -> DIVIDER_TYPE
            is Video -> VIDEO_TYPE
            is AuthorInfo -> AUTHOR_INFO_TYPE
            is Tweet -> TWEET_TYPE
            is Comments -> COMMENT_SUB_TYPE
            is Podcast -> if (item.subtype == INLINE_PODCAST_SUBTYPE) INLINE_PODCAST_TYPE else PODCAST_TYPE
            is ElementGroup -> ELEMENT_GROUP_TYPE
            is Tagline -> TAGLINE_TYPE
            is PullQuote -> PULL_QUOTE_TYPE
            is InterstitialLink -> INTERSTITIAL_LINK_TYPE
            is Ad -> AD_TYPE
            is Audio -> {
                if (item.subtype == HUMAN_AUDIO_SUBTYPE) {
                    HUMAN_AUDIO_TYPE
                } else if (item.subtype == STANDALONE_AUDIO_SUBTYPE) {
                    STANDALONE_AUDIO_TYPE
                } else {
                    AUDIO_TYPE
                }
            }
            is OlympicsMedals -> OLYMPICS_MEDALS
            is Anchor -> ANCHOR
            is Instagram -> INSTAGRAM_TYPE
            is TableItem -> TABLE_TYPE
            is InlineAlertToggleItem -> INLINE_ALERT_TOGGLE_ITEM
            is InlineTopicFollowItem -> INLINE_TOPIC_FOLLOW
            is InlineMessageItem -> INLINE_EXTRA_ACCOUNT_TOGGLE_ITEM
            is ContextBox -> CONTEXT_BOX_TYPE
            is ForYouRecirculationItem -> FOR_YOU_RECIRC_TYPE
            is ExpandCollapseCard -> EXPAND_COLLAPSE_TYPE
            is GalleryExpandCollapse -> GALLERY_EXPAND_COLLAPSE_TYPE
            is LinkButton -> LINK_BUTTON_SUB_TYPE
            is RecirculationItem -> MOST_READ
            is InlineCarousel -> INLINE_CAROUSEL_TYPE
            is LiveOutcome -> LIVE_OUTCOME
            is SubNav -> SUB_NAV
            is FtsCarousel -> FTS_CAROUSEL_TYPE
            is WebEmbed -> OEMBED_TYPE
            is Pdf -> PDF_TYPE
            is AutoRecircCarousel -> AUTO_RECIRC_TYPE
            is BlockQuote -> BLOCK_QUOTE_TYPE
            is Quote -> QUOTE_TYPE
            else -> -1
        }
}
