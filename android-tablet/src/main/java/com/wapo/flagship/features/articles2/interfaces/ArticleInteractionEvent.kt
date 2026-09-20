package com.wapo.flagship.features.articles2.interfaces

import com.wapo.flagship.features.articles2.models.InlineTopicFollowItem
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.navigation_models.ShareContent
import com.wapo.flagship.features.subscribebanner.state.BannerEvent

/**
 * This class consists of all the click events tht will be fired from within the articles activity.
 */
sealed class ArticleInteractionEvent {
    /**
     * This event will be fired when we want to open comments section (currently in a webview) e.g. "View Comments" button is clicked in an article.
     */
    object ViewCommentsClickEvent : ArticleInteractionEvent()

    /**
     * This event will be fired when we want to open a "From the Source" annotation or carousel item.
     */
    class FromTheSourceTapEvent(
        val commentUrl: String,
        val miscellany: String,
        val sourceName: String?,
    ) : ArticleInteractionEvent()

    /**
     * This event will be fired when we want to show the follow dialog e.g. when user taps on the author name in by-line.
     */
    class AuthorNameClickEvent(
        val authorId: String,
    ) : ArticleInteractionEvent()

    /**
     * This event will be fired when we want to show the Ask The Post questions dialog
     */
    class AskThePostClickEvent(
        val questionId: String,
    ) : ArticleInteractionEvent()

    /**
     * This event will be fired when we want to open a linked article within article (Link drilling)
     */
    class LinkClickEvent(
        val url: String,
    ) : ArticleInteractionEvent()

    /**
     * This event will be fired when we want to open a luf outcome post within article
     */
    class LufOutcomePostClickEvent(
        val url: String,
    ) : ArticleInteractionEvent()

    /**
     * Will be triggered when user taps on image in an article and will open up the full screen image.
     */
    class ImageClickEvent(
        val imageUrl: String,
    ) : ArticleInteractionEvent()

    /**
     * Will be triggered when user taps on share when article is in text selectio mode.
     */
    class TextSelectionShareEvent(
        val shareContent: ShareContent,
    ) : ArticleInteractionEvent()

    /**
     * Will be triggered when scroll on individual article is started.
     */
    object ArticleScrollStartedEvent : ArticleInteractionEvent()

    /**
     * Will be triggered when scroll on individual article is stopped (which was previously started)
     */
    object ArticleScrollStoppedEvent : ArticleInteractionEvent()

    class AudioItemClicked(
        val url: String,
        val isActionAudio: Boolean = false,
    ) : ArticleInteractionEvent()

    class TopicFollowButtonClicked(
        val inlineTopicFollowItem: InlineTopicFollowItem,
    ) : ArticleInteractionEvent()

    class AlertToggled(
        val topicKey: String,
        val topicDisplayName: String,
        val isEnabled: Boolean
    ) : ArticleInteractionEvent()

    class ArticleGalleryExpandCollapse(
        val item: Item,
    ) : ArticleInteractionEvent()

    class ArticleTruncateExpandCollapse(
        val item: Item,
    ) : ArticleInteractionEvent()

    class ArticleCardExpandCollapse(
        val item: Item,
    ) : ArticleInteractionEvent()

    class InlineMessageBannerEvent(
        val bannerEvent: BannerEvent
    ) : ArticleInteractionEvent()

    class CarouselCardSwipeEvent(
        val shouldAllowPagerScroll: Boolean,
    ) : ArticleInteractionEvent()

    class CarouselItemClick(
        val url: String
    ) : ArticleInteractionEvent()

    class VideoClickEvent(
        val videoId: String
    ) : ArticleInteractionEvent()

    class VideoAutoplayEvent(
        val videoId: String
    ) : ArticleInteractionEvent()

    class VideoOffscreenEvent(
        val videoId: String
    ) : ArticleInteractionEvent()

    class KickerClickEvent(
        val path: String
    ) : ArticleInteractionEvent()

    class ElementGroupExpandCollapse(
        val group: String,
    ) : ArticleInteractionEvent()

    class DisclaimerInfoClicked(
        val reference: String
    ): ArticleInteractionEvent()
}
