package com.wapo.flagship.features.articles2.tracking

import android.content.Context
import androidx.core.net.toUri
import com.wapo.android.commons.appsFlyer.AppsFlyerMeasurement
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.external.storage.WidgetType
import com.wapo.flagship.features.articles2.events.Article2Events
import com.wapo.flagship.features.articles2.utils.toTrackingInfo
import com.wapo.flagship.features.articles2.viewholders.InStoryRecirculationViewHolder
import com.wapo.flagship.features.notification.NotificationsFragment
import com.wapo.flagship.json.TrackingInfoPageType
import com.wapo.flagship.util.ChartbeatManager
import com.wapo.flagship.util.ChartbeatManager.setAuthors
import com.wapo.flagship.util.JUcidTracker
import com.wapo.flagship.util.UtilsKt
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.MeasurementMap
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.follow.misc.FollowTracking
import com.washingtonpost.android.follow.misc.FollowTrackingInfo
import com.washingtonpost.android.follow.misc.TrackingEvent
import com.washingtonpost.android.save.types.MyPostSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * This class is used for carrying out all analytics tracking related operations.
 * [firebaseTrackingHelperData] is the helper data that is generated when the article is loaded in [Aticle2Activity] for the very first time.
 * This data is not changed when swiping to different pages within the same instance of an activity.
 */
class FirebaseAnalyticsTracker(
    val firebaseTrackingHelperData: FirebaseTrackingHelperData,
) {
    private val config get() = ConfigManager.getInstance().config

    /**
     * On article load and page changed this function is called to start the tracking of analytics.
     * [firebaseTrackingInfo] - article specific tracking info.
     * [context] Context of the current view.
     */
    fun startTracking(
        firebaseTrackingInfo: FirebaseTrackingInfo,
        context: Context,
        jTid: Long?,
        isGiftArticle: Boolean = false,
        isNewsprint: Boolean = false,
        onArticlePageViewTracker: (event: Article2Events.Article2TrackerEvent) -> Unit
    ) {
        val trackingInfo = firebaseTrackingInfo.omnitureX?.toTrackingInfo()
        val omniture = firebaseTrackingInfo.omnitureX
        val measurementMap = MeasurementMap()

        val navigationBehavior =
            when {
                firebaseTrackingHelperData.isPushOriginated -> Measurement.PATH_TO_VIEW_PUSH_NOTIFICATION
                firebaseTrackingHelperData.isPrintOriginated -> {
                    omniture?.toTrackingInfo()?.interfaceType = Measurement.INTERFACE_TYPE_PRINT
                    Measurement.INTERFACE_TYPE_PRINT
                }
                firebaseTrackingHelperData.isCarouselOriginated -> {
                    val isMyPost = MyPostSection.values().any { it.name == firebaseTrackingHelperData.sectionDisplayName }
                    val isInlineCarousel = firebaseTrackingHelperData.sectionDisplayName == InStoryRecirculationViewHolder.SECTION_TITLE
                    val isAutoRecircCarousel = firebaseTrackingHelperData.isFromRecircModule && !firebaseTrackingHelperData.carouselCategoryId.isNullOrEmpty()
                    if (isNewsprint) {
                        firebaseTrackingHelperData.itId
                    } else if (isMyPost) {
                        // Set the values for a page view for a My Post Carousel article click (AWA-5770)
                        Measurement.setMyPostCarouselPosition(
                            measurementMap,
                            firebaseTrackingHelperData.positionInMyPostCarousel,
                        )
                        firebaseTrackingHelperData.navigationBehavior
                    } else if (isInlineCarousel) {
                        String.format(
                            Locale.getDefault(),
                            Measurement.PATH_TO_VIEW_RECIRCULATION_INLINE_CAROUSEL,
                            firebaseTrackingHelperData.carouselTitle,
                            firebaseTrackingInfo.position + 1,
                        )
                    } else if (isAutoRecircCarousel) {
                        String.format(
                            Locale.getDefault(),
                            Measurement.PATH_TO_VIEW_RECIRCULATION_INLINE_CAROUSEL,
                            firebaseTrackingHelperData.carouselCategoryId,
                            firebaseTrackingInfo.position + 1,
                        )
                    } else {
                        String.format(
                            Locale.getDefault(),
                            Measurement.PATH_TO_VIEW_RECIRCULATION_MOST_READ,
                            firebaseTrackingInfo.position + 1,
                        )
                    }
                }
                firebaseTrackingHelperData.isDeeplinkOriginated -> {
                    if (firebaseTrackingHelperData.isWpmmArticle) {
                        Measurement.setWpmmArticleContentId(omniture?.contentId)
                    }
                    if (isGiftArticle) {
                        Measurement.PATH_TO_VIEW_GIFT_ARTICLE
                    } else if (firebaseTrackingHelperData.sourceApp in config.deepLinkConfig.validSourceAppValues) {
                        firebaseTrackingHelperData.sourceApp
                    } else {
                        Measurement.PATH_TO_VIEW_DEEP_LINK
                    }
                }
                firebaseTrackingHelperData.isForYouSectionOriginated &&
                    firebaseTrackingInfo.position == firebaseTrackingHelperData.firstSelectedIndex -> {
                        val appendedStr = if (firebaseTrackingHelperData.isDefaultForYou) "_fy_opened" else ""
                        String.format(
                            Locale.getDefault(),
                            Measurement.PATH_TO_VIEW_FOR_YOU_SECTION,
                            firebaseTrackingHelperData.positionInForYouSection + 1,
                        ) + appendedStr
                    }
                firebaseTrackingHelperData.isAirshipOriginated -> {
                    if (firebaseTrackingHelperData.isWpmmArticle) {
                        Measurement.setWpmmArticleContentId(omniture?.contentId)
                    }
                    Measurement.PATH_TO_VIEW_IN_APP_PROMPT
                }
                firebaseTrackingHelperData.isOneLinkOriginated -> {
                    if (firebaseTrackingHelperData.isWpmmArticle) {
                        Measurement.setWpmmArticleContentId(omniture?.contentId)
                    }
                    if (firebaseTrackingHelperData.sourceApp in config.deepLinkConfig.validSourceAppValues) {
                        firebaseTrackingHelperData.sourceApp
                    } else {
                        Measurement.PATH_TO_VIEW_ONELINK
                    }
                }
                firebaseTrackingHelperData.isInlineLinkOriginated -> {
                    Measurement.PATH_TO_VIEW_INLINE_LINK
                }
                firebaseTrackingHelperData.isLufOutcomePostOriginated -> {
                    Measurement.PATH_TO_VIEW_LUF_OUTCOME_POST
                }
                firebaseTrackingHelperData.widgetData?.isWidgetOriginated == true -> {
                   if (WidgetType.TABLET_WIDGET.name.equals(
                            firebaseTrackingHelperData.widgetData.widgetType,
                            ignoreCase = true,
                        )
                    ) {
                        Measurement.PATH_TO_VIEW_WIDGET
                    } else if (WidgetType.WIDGET.name.equals(
                            firebaseTrackingHelperData.widgetData.widgetType,
                            ignoreCase = true,
                        )
                    ) {
                        Measurement.PATH_TO_VIEW_WIDGET_SMALL
                    } else if (WidgetType.FOR_YOU_WIDGET.name.equals(
                        firebaseTrackingHelperData.widgetData.widgetType,
                        ignoreCase = true,
                        )
                    ) {
                        Measurement.PATH_TO_VIEW_FOR_YOU_WIDGET_LARGE
                    } else {
                        Measurement.PATH_TO_VIEW_UNKNOWN_WIDGET
                    }
                }
                firebaseTrackingHelperData.isFromRelatedArticle -> {
                    Measurement.NAVIGATION_BEHAVIOR_RELATED_ARTICLE
                }
                else -> {
                    if (firebaseTrackingInfo.position == firebaseTrackingHelperData.firstSelectedIndex) {
                        // meaning we just opened an article, need to determine navigation behavior
                        determineNavigationBehavior(measurementMap)
                    } else {
                        // every subsequent swipe should be tracked as SWIPES regardless the original navigation behavior value
                        Measurement.PATH_TO_VIEW_SWIPE
                    }
                }
            }
        if (firebaseTrackingHelperData.isOpenFromSearch) {
            Measurement.setNavigationBehaviorToSearchMap(navigationBehavior)
        }
        setNavigationBehavior(navigationBehavior, measurementMap)

        ChartbeatManager.trackView(
            context,
            getPath(firebaseTrackingInfo.contentUrl),
            omniture?.title ?: omniture?.pageName,
        )
        ChartbeatManager.setSections(omniture?.channel)
        setAuthors(omniture?.contentAuthor?.replace(";".toRegex(), ","))

        // AppsFlyer built-in page view event with no parameters
        AppsFlyerMeasurement.trackEvent(context, AppsFlyerMeasurement.CONTENT_PAGE_VIEW, null)

        if (firebaseTrackingHelperData.widgetData != null && firebaseTrackingHelperData.widgetData.isWidgetOriginated) {
            Measurement.enableWidgetOrigination(firebaseTrackingHelperData.widgetData.widgetType)
        }
        firebaseTrackingInfo.contentWeight?.apply {
            Measurement.setTetroAttributes(this)
        }
        trackingInfo?.apply {
            pageType = TrackingInfoPageType.ARTICLE
            blogName = firebaseTrackingInfo.blogName
            contentURL = firebaseTrackingInfo.contentUrl
            firstPublishedDate = firebaseTrackingInfo.firstPublishedDate?.let { Date(it) }
            firstPublishedTime = firebaseTrackingInfo.firstPublishedDate
            lastModifiedTime = firebaseTrackingInfo.lastModifiedTime
            commercialNode = firebaseTrackingInfo.commercialNode
            // In Targeting API v1, `permutive` is a list.
            // In Targeting API v1.5 (current), `permutiveDict` is a map and `permutive` is a list.
            // In Targeting API v2, `permutiveDict` is not available and `permutive` has the same type and copy of `permutiveDict` data.
            // TODO Clean up 'permutiveDict' once it is removed from the Targeting API and feeds are updated.
            targetingDict = firebaseTrackingInfo.targeting?.permutiveDict ?: firebaseTrackingInfo.targeting?.permutive
        }
        Measurement.setJUcid(measurementMap, JUcidTracker.jUcid)
        Measurement.setJTid(measurementMap, jTid)
        if (firebaseTrackingHelperData.currentTabName != null &&
            firebaseTrackingHelperData.currentTabName.lowercase(
                Locale.US,
            ) == FOLLOWING_TAB_NAME
        ) {
            FollowTrackingInfo.followTracking =
                FollowTracking(
                    firebaseTrackingInfo.omnitureX?.pageName,
                    firebaseTrackingInfo.omnitureX?.channel,
                    firebaseTrackingInfo.omnitureX?.contentAuthor,
                    firebaseTrackingInfo.omnitureX?.authorId,
                    "",
                    firebaseTrackingHelperData.currentTabName,
                    firebaseTrackingHelperData.sectionDisplayName,
                    "",
                    firebaseTrackingInfo.omnitureX?.subSection,
                )
            FlagshipApplication.getInstance().followProvider.onTrackingEvent(
                TrackingEvent.ON_READ_ARTICLE_FROM_FOLLOWING_FEED,
            )

            Measurement.setInlinePushToggleFlag(
                measurementMap,
                firebaseTrackingInfo.inlinePushToggleFlag,
            )
            Measurement.trackWithTrackingInfo(
                trackingInfo,
                firebaseTrackingInfo.position,
                MY_POST,
                FOLLOWING_TAB_NAME,
                firebaseTrackingHelperData.pushArticleTrackingHelperData,
                measurementMap,
            )
        } else {
            Measurement.setInlinePushToggleFlag(
                measurementMap,
                firebaseTrackingInfo.inlinePushToggleFlag,
            )

            if (trackingInfo != null) {
                onArticlePageViewTracker(
                    Article2Events.Article2TrackerEvent(
                        omniture = trackingInfo,
                        position = firebaseTrackingInfo.position,
                        currentAppTab = firebaseTrackingHelperData.currentTabName ?: "",
                        currentAppSection = firebaseTrackingHelperData.sectionDisplayName ?: "",
                        pushTrackingHelperData = firebaseTrackingHelperData.pushArticleTrackingHelperData,
                        measurementMap = measurementMap
                    )
                )
            }
        }

        // Logging missing j_ucid and j_tid values for article pvs and will be removed
        // once there are no mismatch values between ad and pv requests.
        if (trackingInfo?.getjUcid().isNullOrEmpty()) {
            EventLog
                .Builder()
                .apply {
                    setMessage("j_ucid is null in a pv")
                    setModule(LogModules.ARTICLES)
                    setContentUrl(trackingInfo?.contentURL)
                }.run {
                    RemoteLog.w(context, build())
                }
        } else {
            // LogUtil.d("JUCID", "j_ucid from article pv: ${trackingInfo?.getjUcid()}")
        }
        if (jTid == null || jTid == 0L) {
            EventLog
                .Builder()
                .apply {
                    setMessage("j_tid is null in a pv")
                    setModule(LogModules.ARTICLES)
                    setContentUrl(trackingInfo?.contentURL)
                }.run {
                    RemoteLog.w(context, build())
                }
        } else {
            // LogUtil.d("JTID", "j_tid from article pv: $jTid")
        }
    }

    private fun setNavigationBehavior(
        navigationBehavior: String?,
        measurementMap: MeasurementMap,
    ) {
        if (navigationBehavior == null) {
            Measurement.setNavigationBehavior(
                measurementMap,
                determineNavigationBehavior(measurementMap),
            )
        } else {
            Measurement.setNavigationBehavior(
                Measurement.getDefaultMap(),
                navigationBehavior,
            )
        }
    }

    private fun determineNavigationBehavior(extras: MeasurementMap): String {
        var navigationBehavior =
            firebaseTrackingHelperData.itId // Use itId if it's available - Top Stories uses itId
                ?: firebaseTrackingHelperData.omnitureToPathView
                ?: firebaseTrackingHelperData.navigationBehavior
                ?: firebaseTrackingHelperData.sectionDisplayName?.let {
                    "${Measurement.PATH_TO_VIEW_SECTION_PREFIX}_${UtilsKt.toAnalyticsSnakeCase(it)}"
                }
                ?: Measurement.PATH_TO_VIEW_FRONT

        when (navigationBehavior) {
            Measurement.PATH_TO_VIEW_FRONTS_CAROUSEL ->
                Measurement.setBrightsCarouselPosition(
                    extras,
                    firebaseTrackingHelperData.positionInCarousel,
                )
            Measurement.PATH_TO_VIEW_FRONTS_STACK ->
                Measurement.setPositionInStack(
                    extras,
                    firebaseTrackingHelperData.positionInCarousel,
                )
            Measurement.PATH_TO_VIEW_FRONTS_CAROUSEL_IMMERSION ->
                navigationBehavior +=
                    "_" + (firebaseTrackingHelperData.positionInCarousel + 1)
        }
        return navigationBehavior
    }

    /**
     * Tracks when back button is pressed from an article.
     */
    fun backPressTrack() {
        if (firebaseTrackingHelperData.isAlertOriginated) {
            NotificationsFragment.navigationBehavior = Measurement.PATH_TO_VIEW_BACK_TO_FRONT
        } else {
            Measurement.setSubsection(
                Measurement.getDefaultMap(),
                firebaseTrackingHelperData.sectionDisplayName,
            )
            Measurement.setPageName(
                Measurement.getDefaultMap(),
                Measurement.getTrackingPageName(
                    firebaseTrackingHelperData.sectionDisplayName,
                    firebaseTrackingHelperData.currentTabName,
                ),
            )
            Measurement.setNavigationBehavior(
                Measurement.getDefaultMap(),
                Measurement.PATH_TO_VIEW_BACK_TO_FRONT,
            )
            val map = Measurement.getNewMap()
            Measurement.setJTid(map, System.currentTimeMillis())
            Measurement.setJUcid(Measurement.getDefaultMap(), JUcidTracker.jUcid)
            Measurement.trackEvent(map, Events.EVENT_PAGE_VIEW)
        }
    }

    /**
     * Tracks when article is bookmarked / added to Saved Stories.
     * [firebaseTrackingInfo] tracking info required to proceed with this tracking.
     * @param isSaving true - if saving to favorites, false - if removing from favorites
     */
    fun bookmarkTrack(
        firebaseTrackingInfo: FirebaseTrackingInfo,
        isSaving: Boolean,
    ) {
        Measurement.trackSaveUnsave(
            firebaseTrackingInfo.title,
            firebaseTrackingInfo.omnitureX?.arcId ?: firebaseTrackingInfo.omnitureX?.contentId,
            firebaseTrackingInfo.omnitureX?.contentType,
            firebaseTrackingHelperData.currentTabName,
            false,
            isSaving,
            false,
            firebaseTrackingInfo.contentUrl,
        )
    }

    /**
     * Tracks when article is being gifted. Tracks gift sending events.
     * [firebaseTrackingInfo] tracking info required to proceed with this tracking.
     * [details] differentiates different sender flow clicks (Tool bar icon click, Share click, No gifts left screen)
     */
    fun giftClickTrack(
        contentUrl: String,
        firebaseTrackingInfo: FirebaseTrackingInfo?,
        details: String,
    ) {
        Measurement.trackGiftSendClicked(
            contentUrl,
            firebaseTrackingInfo?.omnitureX,
            details,
        )
    }

    /**
     * Tracks when user taps on view comment button in an article.
     * [firebaseTrackingInfo] tracking info.
     * [appSection] current app section.
     */
    fun trackCommentsClick(firebaseTrackingInfo: FirebaseTrackingInfo) {
        Measurement.trackCommentsButtonClick(
            firebaseTrackingInfo.omnitureX?.toTrackingInfo().apply {
                this?.firstPublishedDate =
                    firebaseTrackingInfo.firstPublishedDate?.let { Date(it) }
            },
            firebaseTrackingHelperData.sectionDisplayName,
        )
    }

    /**
     * Tracks the scroll events on individual articles.
     * [firebaseTrackingInfo] tracking info.
     */
    fun trackScrollEvents(
        firebaseTrackingInfo: FirebaseTrackingInfo,
        articleScrollEventType: Events,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            Measurement.trackArticleScrollEvent(
                firebaseTrackingInfo.omnitureX?.toTrackingInfo(),
                firebaseTrackingHelperData.currentTabName,
                articleScrollEventType,
                firebaseTrackingHelperData.pushArticleTrackingHelperData,
            )
        }
    }

    /**
     * Tracks showing of author info dialog with follow button.
     * [firebaseTrackingInfo] tracking info.
     */
    fun trackAuthorInfoDialogShownEvent(firebaseTrackingInfo: FirebaseTrackingInfo) {
        FollowTrackingInfo.followTracking =
            FollowTracking(
                firebaseTrackingInfo.omnitureX?.pageName,
                firebaseTrackingInfo.omnitureX?.channel,
                firebaseTrackingInfo.omnitureX?.contentAuthor,
                firebaseTrackingInfo.omnitureX?.authorId,
                "",
                firebaseTrackingHelperData.currentTabName,
                firebaseTrackingHelperData.sectionDisplayName,
                "",
                firebaseTrackingInfo.omnitureX?.subSection,
            )
        FlagshipApplication.getInstance().followProvider.onTrackingEvent(
            TrackingEvent.ON_AUTHOR_CARD_OPEN,
        )
    }

    fun trackArticleSummaryEvent(
        firebaseTrackingInfo: FirebaseTrackingInfo,
        summaryScreenSeen: Boolean,
        feedbackScreenSeen: Boolean,
        feedbackSubmit: Boolean,
        navigationBehavior: String?,
        miscellanySuffix: String?,
    ) {
        if (summaryScreenSeen) {
            Measurement.trackArticleSummarySeenEvent(
                firebaseTrackingInfo.omnitureX?.pageName,
                firebaseTrackingInfo.omnitureX?.arcId,
                navigationBehavior,
                miscellanySuffix,
            )
        } else if (feedbackScreenSeen) {
            Measurement.trackArticleSummaryFeedbackSeenEvent(
                firebaseTrackingInfo.omnitureX?.pageName,
                firebaseTrackingInfo.omnitureX?.arcId,
                navigationBehavior,
                miscellanySuffix,
            )
        } else if (feedbackSubmit) {
            Measurement.trackArticleSummaryFeedbackSubmitEvent(
                firebaseTrackingInfo.omnitureX?.pageName,
                firebaseTrackingInfo.omnitureX?.arcId,
                navigationBehavior,
                miscellanySuffix,
            )
        }
    }

    fun trackOnPageTap(
        firebaseTrackingInfo: FirebaseTrackingInfo,
        miscellany: String,
        genEventDimension: String?
    ) {
        Measurement.trackOnpageTap(
            miscellany,
            firebaseTrackingInfo.omnitureX?.pageName,
            firebaseTrackingInfo.omnitureX?.arcId,
            genEventDimension
        )
    }

    private fun getPath(url: String?): String? =
        try {
            url?.toUri()?.path ?: url
        } catch (e: Exception) {
            url
        }

    companion object {
        const val FOLLOWING_TAB_NAME = "following"
        const val MY_POST = "my post"
    }
}
