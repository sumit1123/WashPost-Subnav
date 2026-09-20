package com.wapo.flagship.features.articles2.tracking

/**
 * This data class holds all the necessary info that is required for firebase analytics tracking in [FirebaseAnalyticsTracker]
 */
data class FirebaseTrackingHelperData(
    val omnitureToPathView: String? = null,
    val widgetData: FirebaseTrackingHelperWidgetData? = null,
    val isPushOriginated: Boolean = false,
    val isPrintOriginated: Boolean = false,
    val isCarouselOriginated: Boolean = false,
    val isDeeplinkOriginated: Boolean = false,
    val isForYouSectionOriginated: Boolean = false,
    val isAirshipOriginated: Boolean = false,
    val isOneLinkOriginated: Boolean = false,
    val isHabitTilesOriginated: Boolean = false,
    val isAskThePostOriginated: Boolean = false,
    val isWpmmArticle: Boolean = false,
    val isAlertOriginated: Boolean = false,
    val sectionDisplayName: String?,
    val currentTabName: String?,
    val firstSelectedIndex: Int,
    val navigationBehavior: String?,
    val pushArticleTrackingHelperData: PushArticleTrackingHelperData?,
    val isInlineLinkOriginated: Boolean = false,
    val isLufOutcomePostOriginated: Boolean = false,
    val positionInCarousel: Int = 0,
    val positionInMyPostCarousel: Int = 0,
    val positionInForYouSection: Int = 0,
    val itId: String? = null,
    val sourceApp: String? = null,
    val carouselTitle: String? = null,
    val carouselCategoryId: String? = null,
    val isOpenFromSearch: Boolean = false,
    val isDefaultForYou: Boolean = false,
    val isFromRelatedArticle: Boolean = false,
    val isFromRecircModule: Boolean = false,
)
