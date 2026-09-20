package com.wapo.flagship.features.articles2.tracking

import com.wapo.flagship.util.tracking.Events

sealed class FirebaseAnalyticsTrackingEvent {
    /**
     * This event is fired when the article is loaded or swiping through list of articles.
     * [firebaseTrackingInfo] tracking info.
     * [jTid] The id to uniquely identify the pageview at a specific time.
     */
    class ArticleTracking(
        val firebaseTrackingInfo: FirebaseTrackingInfo,
        val jTid: Long?,
    ) : FirebaseAnalyticsTrackingEvent()

    /**
     * This event is fired when activity's onBackPressed is called.
     */
    data object BackPressTracking : FirebaseAnalyticsTrackingEvent()

    /**
     * This event is fired when an article is bookmarked/added to Saved Stories.
     * [firebaseTrackingInfo] tracking info.
     * @param isSaving true - if saving to favorites, false - if removing from favorites
     */
    class BookmarkTracking(
        val firebaseTrackingInfo: FirebaseTrackingInfo,
        val isSaving: Boolean,
    ) : FirebaseAnalyticsTrackingEvent()

    /**
     * This event is fired when user taps on the "View Comments" button in an article
     * [firebaseTrackingInfo] tracking info.
     */
    class ViewCommentsTracking(
        val firebaseTrackingInfo: FirebaseTrackingInfo,
    ) : FirebaseAnalyticsTrackingEvent()

    /**
     * This event is fired when user scrolls on an individual article.
     * [firebaseTrackingInfo] tracking info.
     * [articleScrollEventType] whether scroll was started or stopped.
     */
    class ArticleScrollTracking(
        val articleScrollEventType: Events,
        val firebaseTrackingInfo: FirebaseTrackingInfo,
    ) : FirebaseAnalyticsTrackingEvent()

    /**
     * This event is fired when user taps on author name in byline
     * [firebaseTrackingInfo] tracking info.
     */
    class AuthorFollowCardShownTracking(
        val firebaseTrackingInfo: FirebaseTrackingInfo,
    ) : FirebaseAnalyticsTrackingEvent()

    /**
     * This event is fired when user taps on author name in byline
     * [firebaseTrackingInfo] tracking info.
     */
    class GiftSenderTracking(
        val contentUrl: String,
        val firebaseTrackingInfo: FirebaseTrackingInfo?,
        val details: String,
    ) : FirebaseAnalyticsTrackingEvent()

    class ArticleSummaryTracking(
        val firebaseTrackingInfo: FirebaseTrackingInfo,
        val summaryScreenSeen: Boolean = false,
        val feedbackScreenSeen: Boolean = false,
        val feedbackSubmit: Boolean = false,
        val navigationBehavior: String? = null,
        val miscellanySuffix: String? = null
    ) : FirebaseAnalyticsTrackingEvent()

    class OnPageTap(
        val firebaseTrackingInfo: FirebaseTrackingInfo,
        val miscellany: String,
        val genEventDimension: String?
    ) : FirebaseAnalyticsTrackingEvent()
}
