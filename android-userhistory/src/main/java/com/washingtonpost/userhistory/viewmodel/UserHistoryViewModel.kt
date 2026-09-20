// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.washingtonpost.userhistory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.android.commons.util.UiUtils
import com.wapo.android.domain.repository.RemoteLogRepo
import com.washingtonpost.userhistory.ForYouViewedAction
import com.washingtonpost.userhistory.HeadlineViewAction
import com.washingtonpost.userhistory.domain.UserHistoryPrefRepo
import com.washingtonpost.userhistory.models.ForYouViewedHelperItem
import com.washingtonpost.userhistory.models.ForYouViewedItem
import com.washingtonpost.userhistory.models.HabitTileViewItem
import com.washingtonpost.userhistory.models.PageViewItem
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.washingtonpost.userhistory.models.ScrollDepthItem
import com.washingtonpost.userhistory.models.UserHistoryArticleItem
import com.washingtonpost.userhistory.models.VideoConclusionState
import com.washingtonpost.userhistory.domain.UserHistoryManager
import com.washingtonpost.userhistory.models.AutoRecircHelperItem
import com.washingtonpost.userhistory.models.DefaultHeadlineViewEvent
import com.washingtonpost.userhistory.models.HeadlineViewHelperItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UserHistoryViewModel @Inject constructor(
    private val userHistoryManager: UserHistoryManager,
    private val userHistoryPrefRepo: UserHistoryPrefRepo,
    private val remoteLog: RemoteLogRepo,
    private val deviceUtilRepo: DeviceUtilRepo
) : ViewModel() {
    private var pageViewId: String = UUID.randomUUID().toString()
    private var forYouFeedSessionId: String = UUID.randomUUID().toString()

    fun postUserHistoryEvents() {
        viewModelScope.launch {
            userHistoryManager.postUserHistoryEvents()
        }
    }

    fun postPageViewEvent() {
        viewModelScope.launch {
            userHistoryManager.postPageViewEvent()
        }
    }

    fun postPushEvent(
        url: String,
        pushId: String?,
        pushType: String,
        testGroup: String,
        isPushClicked: Boolean,
        loginId: String?
    ) {
        viewModelScope.launch {
            userHistoryManager.postPushEvent(
                url,
                pushId,
                pushType,
                testGroup,
                isPushClicked,
                loginId
            )
        }
    }

    fun writeVideoViewedEvent(
        id: String?,
        section: String,
        autoplayDuration: Long?,
        watchDuration: Long?,
        totalMilliSeconds: Long?,
        videoConclusionState: VideoConclusionState
    ) {
        userHistoryManager.writeVideoViewedEvent(
            id,
            section,
            autoplayDuration,
            watchDuration,
            totalMilliSeconds,
            videoConclusionState,
        )
    }

    //region scroll_depth
    private var scrollDepthItem: ScrollDepthItem? = null

    /**
     * The positions of the valid scroll depth content items in the list of all items in the article
     */
    private var contentItemIds = mutableListOf<String>()

    fun initializeScrollDepth(
        articleId: String,
        items: List<UserHistoryArticleItem>?,
        deepestScrollId: String?
    ) {
        if (scrollDepthItem != null) return
        // Set up contentItemIds
        contentItemIds.clear()
        items?.forEach { item ->
            if (item.arcId != null && INCLUDED_CONTENT_TYPES.contains(item.type)) {
                contentItemIds.add(item.arcId)
            }
        }

        if (contentItemIds.isEmpty()) {
            return
        }

        val jucId = userHistoryManager.getJucId()
        val consentGiven = userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven
        //  Log only if jucId is null
        if (jucId.isNullOrEmpty() && consentGiven) {
            val eventLogBuilder = EventLog
                .Builder()
                .setMessage("Breadcrumb - j_ucid=${jucId} at ScrollDepth")
                .set("consentGiven", consentGiven)
                .setModule(LogModules.FOR_YOU)
            remoteLog.e(eventLogBuilder.build())
        }
        // Initialize scrollDepthItem if scroll items exists but initialize with previous scroll depth info
        val scrollIndex = if (contentItemIds.contains(deepestScrollId)) {
            contentItemIds.indexOf(deepestScrollId)
        } else {
            0
        }
        scrollDepthItem =
            ScrollDepthItem(
                articleId = articleId,
                wapoLoginId = userHistoryManager.getWapoLoginId(),
                jucId = jucId,
                jtId = userHistoryManager.getJtId(),
                readingTimeMilli = 0,
                clientEventTime = userHistoryManager.getClientEventTime(),
                pageViewId = pageViewId,
                totalElements = contentItemIds.size,
                deepestScrollIndex = scrollIndex,
                deepestScrollId = contentItemIds[scrollIndex],
                mostRecentScrollIndex = scrollIndex,
                mostRecentScrollId = contentItemIds[scrollIndex]
            )

        scrollDepthItem?.let {
            userHistoryPrefRepo.writeScrollDepthEventToStorage(it)
        }

        var loggableContentIds = ""
        contentItemIds.forEach { loggableContentIds += "$it\n" }
        Logger.d(
            TAG,
            "scroll_depth initializeScrollDepth()\n" +
                    "articleId: $articleId\n" +
                    "total_elements: ${contentItemIds.size}\n" +
                    "ids:\n$loggableContentIds",
        )
    }

    fun updateScrollDepth(id: String) {
        scrollDepthItem?.let {
            val mostRecentScrollIndex = contentItemIds.indexOf(id) + 1
            it.mostRecentScrollIndex = mostRecentScrollIndex
            it.mostRecentScrollId = id
            if (mostRecentScrollIndex > it.deepestScrollIndex) {
                it.deepestScrollIndex = mostRecentScrollIndex
                it.deepestScrollId = id
            }

            Logger.d(
                TAG,
                "scroll_depth updateScrollDepth()\n" +
                        "articleId: ${it.articleId}\n" +
                        "total_elements: ${contentItemIds.size}\n" +
                        "most_recent_scroll_index: ${it.mostRecentScrollIndex}\n" +
                        "most_recent_scroll_id: ${it.mostRecentScrollId}\n" +
                        "deepest_scroll_index: ${it.deepestScrollIndex}\n" +
                        "deepest_scroll_id: ${it.deepestScrollId}",
            )
        }
    }

    fun finalizeScrollDepthItem(startTime: Long) {
        scrollDepthItem?.let {
            setArticleReadingTime(startTime)
            userHistoryPrefRepo.updateScrollDepth(
                it.pageViewId,
                it.deepestScrollIndex,
                it.deepestScrollId,
                System.currentTimeMillis() - it.readingStartTimeMilli,
                userHistoryManager.getClientEventTime()
            )
        }

        scrollDepthItem = null
    }

    private fun setArticleReadingTime(startTime: Long) {
        scrollDepthItem?.readingTimeMilli = System.currentTimeMillis() - startTime
    }
    //endregion scroll_depth

    //region fy_viewed

    /**
     * List of pairs of [ForYouViewedItem]s and their start times
     */
    private var forYouViewedItemsWithStartTimes = mutableListOf<ForYouViewedHelperItem>()

    fun startOrStopForYouViewedTimers(
        recommendationsItems: List<RecommendationsHelperItem>,
        recyclerView: RecyclerView,
        surface: String
    ) {
        val visibleItemCount = recyclerView.childCount
        for (i in 0 until visibleItemCount) {
            val rvItemView = recyclerView.getChildAt(i)
            val adapterPosition = recyclerView.getChildAdapterPosition(rvItemView)
            if (adapterPosition == RecyclerView.NO_POSITION) {
                continue
            }

            if (surface == SURFACE_FEED) {
                if (UiUtils.calculateVerticalVisiblePercentage(rvItemView) >= 50) {
                    startForYouViewTimer(recommendationsItems, adapterPosition, surface)
                } else {
                    stopForYouViewTimer(recommendationsItems, adapterPosition)
                }
            } else if (surface == SURFACE_RECIRC) {
                if (UiUtils.calculateHorizontalVisiblePercentage(rvItemView) >= 50) {
                    startForYouViewTimer(recommendationsItems, adapterPosition, surface)
                } else {
                    stopForYouViewTimer(recommendationsItems, adapterPosition)
                }
            }
        }
    }

    // overloaded method for compose
    fun startOrStopForYouViewedTimers(
        recommendationsItems: List<RecommendationsHelperItem>,
        visibleItemIndices: List<Int>,
        surface: String
    ) {
        for (index in recommendationsItems.indices) {
            if (surface == SURFACE_FEED) {
                if (index in visibleItemIndices) {
                    startForYouViewTimer(recommendationsItems, index, surface)
                } else {
                    stopForYouViewTimer(recommendationsItems, index)
                }
            } else if (surface == SURFACE_RECIRC) {
                if (index in visibleItemIndices) {
                    startForYouViewTimer(recommendationsItems, index, surface)
                } else {
                    stopForYouViewTimer(recommendationsItems, index)
                }
            }
        }
    }

    fun stopAllForYouViewedTimers() {
        forYouViewedItemsWithStartTimes.forEach {
            it.startTime?.let { startTime ->
                if (it.forYouViewedItem.readingTimeMilli == null) {
                    it.forYouViewedItem.readingTimeMilli = System.currentTimeMillis() - startTime
                    userHistoryPrefRepo.writeForYouViewedItemToStorage(it.forYouViewedItem)
                    Logger.d(
                        TAG,
                        "fy_viewed stop timer\narticleId: ${it.forYouViewedItem.articleId}\nreadingTimeSec: ${it.forYouViewedItem.readingTimeMilli}",
                    )
                }
            }
        }
    }

    private fun startForYouViewTimer(
        recommendationsItems: List<RecommendationsHelperItem>,
        adapterPosition: Int,
        surface: String
    ) {
        recommendationsItems.getOrNull(adapterPosition)?.let { recommendationsItem ->
            forYouViewedItemsWithStartTimes.lastOrNull { it.forYouViewedItem.articleId == recommendationsItem.articleId }
                ?.let {
                    if (it.startTime == null) {
                        it.startTime = System.currentTimeMillis()
                        Logger.d(
                            TAG,
                            "fy_viewed start timer\narticleId: ${recommendationsItem.articleId}"
                        )
                    } else if (it.forYouViewedItem.readingTimeMilli != null) {
                        // We already stopped the timer on this item, so create a new event
                        addForYouView(recommendationsItem, adapterPosition, true, surface)
                    } else {
                        // No action
                    }
                } ?: run {
                addForYouView(recommendationsItem, adapterPosition, true, surface)
            }
        }
    }

    private fun stopForYouViewTimer(
        recommendationsItems: List<RecommendationsHelperItem>,
        adapterPosition: Int,
    ) {
        recommendationsItems.getOrNull(adapterPosition)?.let { recommendationsItem ->
            forYouViewedItemsWithStartTimes.lastOrNull { it.forYouViewedItem.articleId == recommendationsItem.articleId }
                ?.let { item ->
                    item.startTime?.let { startTime ->
                        if (item.forYouViewedItem.readingTimeMilli == null) {
                            item.forYouViewedItem.readingTimeMilli =
                                System.currentTimeMillis() - startTime
                            userHistoryPrefRepo.writeForYouViewedItemToStorage(item.forYouViewedItem)
                            if (item.forYouViewedItem.videoDuration != 0L) {
                                writeVideoViewedEvent(
                                    item.forYouViewedItem.articleId ?: "",
                                    "foryou_tab",
                                    item.forYouViewedItem.autoplayDuration,
                                    null,
                                    item.forYouViewedItem.videoDuration,
                                    videoConclusionState = VideoConclusionState.SCROLLED_THROUGH
                                )
                            }
                            Logger.d(
                                TAG,
                                "fy_viewed stop timer\narticleId: ${recommendationsItem.articleId}\nreadingTimeSec: ${item.forYouViewedItem.readingTimeMilli}",
                            )
                        }
                    }
                }
        }
    }

    fun captureForYouViewAction(
        action: ForYouViewedAction,
        recommendationsItem: RecommendationsHelperItem,
        adapterPosition: Int,
        surface: String
    ) {
        forYouViewedItemsWithStartTimes.lastOrNull { it.forYouViewedItem.articleId == recommendationsItem.articleId }
            ?.let {
                setAction(it.forYouViewedItem, action)
            } ?: run {
            addForYouView(recommendationsItem, adapterPosition, false, surface, action)
        }
        Logger.d(
            TAG,
            "fy_viewed action\n" +
                    "articleId: ${recommendationsItem.articleId}\n" +
                    "action: $action",
        )
    }

    fun updateAutoPlayDuration(
        recommendationsItem: RecommendationsHelperItem,
        duration: Long,
        videoDuration: Long
    ) {
        forYouViewedItemsWithStartTimes.lastOrNull { it.forYouViewedItem.articleId == recommendationsItem.articleId }
            ?.let {
                it.forYouViewedItem.autoplayDuration = duration
                it.forYouViewedItem.videoDuration = videoDuration
            }
    }

    private fun addForYouView(
        recommendationsItem: RecommendationsHelperItem,
        adapterPosition: Int,
        shouldStartTimer: Boolean,
        surface: String,
        action: ForYouViewedAction? = null,
    ) {
        val jucId = userHistoryManager.getJucId()
        val consentGiven = userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven
        //  Log only if jucId is null
        if (jucId.isNullOrEmpty() && consentGiven) {
            val eventLogBuilder = EventLog
                .Builder()
                .setMessage("Breadcrumb - j_ucid=${jucId} at ForYouView")
                .set("consentGiven", consentGiven)
                .setModule(LogModules.FOR_YOU)
            remoteLog.e(eventLogBuilder.build())
        }
        val forYouViewedItem =
            ForYouViewedItem(
                articleId = recommendationsItem.articleId,
                wapoLoginId = userHistoryManager.getWapoLoginId(),
                jucId = jucId,
                jtId = userHistoryManager.getJtId(),
                readingTimeMilli = null,
                forYouEvent = recommendationsItem.requestId,
                surface = surface,
                surfaceVariant = getSurfaceVariant(),
                pageViewId = pageViewId,
                recipeId = recommendationsItem.recipeId,
                testId = recommendationsItem.testId,
                recReason = recommendationsItem.recReason,
                position = adapterPosition + 1,
                clientEventTime = userHistoryManager.getClientEventTime(),
                contentType = recommendationsItem.contentType,
                forYouFeedSessionId = forYouFeedSessionId
            )

        setAction(forYouViewedItem, action)

        val forYouViewedHelperItem =
            if (shouldStartTimer) {
                ForYouViewedHelperItem(
                    forYouViewedItem,
                    adapterPosition,
                    System.currentTimeMillis()
                )
            } else {
                ForYouViewedHelperItem(forYouViewedItem, adapterPosition)
            }

        forYouViewedItemsWithStartTimes.add(forYouViewedHelperItem)
        Logger.d(
            TAG,
            "fy_viewed event_created\n" +
                    "articleId: ${recommendationsItem.articleId}\n" +
                    "requestId: ${recommendationsItem.requestId}\n" +
                    "recipeId: ${recommendationsItem.recipeId}\n" +
                    "testId: ${recommendationsItem.testId}\n" +
                    "timer started: $shouldStartTimer\n" +
                    "action: $action"
        )
    }

    /**
     * Builds a PushNotificationViewItem and writes it to Pref
     */
    fun writePushListenerEvent(
        url: String, pushId: String?, pushType: String, testGroup: String, loginId: String?, isPushClicked: Boolean,
    ) {
        userHistoryManager.writePushListenerEvent(
            url,
            pushId,
            pushType,
            testGroup,
            loginId,
            isPushClicked,
        )
    }

    private fun setAction(
        forYouViewedItem: ForYouViewedItem,
        action: ForYouViewedAction?,
    ) {
        when (action) {
            ForYouViewedAction.CLICKED -> {
                forYouViewedItem.apply { clicked = true }
            }

            ForYouViewedAction.LISTENED -> {
                forYouViewedItem.apply { listened = true }
            }

            ForYouViewedAction.SHARED -> {
                forYouViewedItem.apply { shared = true }
            }

            ForYouViewedAction.ADDED_PLAYLIST -> {
                forYouViewedItem.apply { addedPlaylist = true }
            }

            ForYouViewedAction.SAVED_STORY -> {
                forYouViewedItem.apply { savedStory = true }
            }

            ForYouViewedAction.GIFTED -> {
                forYouViewedItem.apply { gifted = true }
            }

            else -> { /* No action */
            }
        }
    }

    //endregion fy_viewed

    //  region auto_recirculation

    private val autoRecircViewedItemsWithStartTimesByCarouselId: MutableMap<String, MutableList<HeadlineViewHelperItem>> = mutableMapOf()

    fun startOrStopAutoRecircViewedTimers(
        carouselId: String,
        recommendationsItems: List<AutoRecircHelperItem>,
        recyclerView: RecyclerView,
    ) {
        val visibleItemCount = recyclerView.childCount
        for (i in 0 until visibleItemCount) {
            val rvItemView = recyclerView.getChildAt(i)
            val adapterPosition = recyclerView.getChildAdapterPosition(rvItemView)
            if (adapterPosition == RecyclerView.NO_POSITION) {
                continue
            }

            if (UiUtils.calculateHorizontalVisiblePercentage(rvItemView) >= 50) {
                startAutoRecircViewTimer(carouselId, recommendationsItems, adapterPosition)
            } else {
                stopAutoRecircViewTimer(carouselId, recommendationsItems, adapterPosition)
            }
        }
    }

    // overloaded method for compose
    fun startOrStopAutoRecircViewedTimers(
        recommendationsItems: List<AutoRecircHelperItem>,
        visibleItemIndices: List<Int>
    ) {
        for (index in recommendationsItems.indices) {
            if (index in visibleItemIndices) {
                startAutoRecircViewTimer(recommendationsItems, index)
            } else {
                stopAutoRecircViewTimer(recommendationsItems, index)
            }
        }
    }

    /** Stops all timers across every carousel. Used by Compose callers that have no carouselId. */
    fun stopAllAutoRecircViewedTimers() {
        autoRecircViewedItemsWithStartTimesByCarouselId.values.flatten().forEach {
            it.startTime?.let { startTime ->
                if (it.headlineViewEvent.readingTimeMilli == null) {
                    it.headlineViewEvent.readingTimeMilli = System.currentTimeMillis() - startTime
                    userHistoryPrefRepo.writeHeadlineViewItemToStorage(it.headlineViewEvent)
                    Logger.d(
                        TAG,
                        "headline_view (auto_recirc) stop timer\narticleId: ${it.headlineViewEvent.articleId}\nreadingTimeSec: ${it.headlineViewEvent.readingTimeMilli}",
                    )
                }
            }
        }
    }

    fun stopAllAutoRecircViewedTimers(carouselId: String) {
        autoRecircViewedItemsWithStartTimesByCarouselId
            .getOrPut(carouselId, { mutableListOf() })
            .forEach {
                it.startTime?.let { startTime ->
                    if (it.headlineViewEvent.readingTimeMilli == null) {
                        it.headlineViewEvent.readingTimeMilli = System.currentTimeMillis() - startTime
                        userHistoryPrefRepo.writeHeadlineViewItemToStorage(it.headlineViewEvent)
                        Logger.d(
                            TAG,
                            "headline_view (auto_recirc) stop timer\narticleId: ${it.headlineViewEvent.articleId}\nreadingTimeSec: ${it.headlineViewEvent.readingTimeMilli}",
                        )
                    }
                }
            }
    }

    private fun startAutoRecircViewTimer(
        carouselId: String,
        recommendationsItems: List<AutoRecircHelperItem>,
        adapterPosition: Int,
    ) {
        recommendationsItems.getOrNull(adapterPosition)?.let { recommendationsItem ->
            autoRecircViewedItemsWithStartTimesByCarouselId
                .getOrPut(carouselId, { mutableListOf() })
                .lastOrNull { it.headlineViewEvent.articleId == recommendationsItem.articleId }
                ?.let {
                    if (it.startTime == null) {
                        it.startTime = System.currentTimeMillis()
                        Logger.d(
                            TAG,
                            "headline_view (auto_recirc) start timer\narticleId: ${recommendationsItem.articleId}"
                        )
                    } else if (it.headlineViewEvent.readingTimeMilli != null) {
                        // We already stopped the timer on this item, so create a new event
                        addAutoRecircView(carouselId, recommendationsItem, adapterPosition, true)
                    } else {
                        // No action
                    }
                } ?: run {
                addAutoRecircView(carouselId, recommendationsItem, adapterPosition, true)
            }
        }
    }

    private fun stopAutoRecircViewTimer(
        carouselId: String,
        recommendationsItems: List<AutoRecircHelperItem>,
        adapterPosition: Int,
    ) {
        recommendationsItems.getOrNull(adapterPosition)?.let { recommendationsItem ->
            autoRecircViewedItemsWithStartTimesByCarouselId
                .getOrPut(carouselId, { mutableListOf() })
                .lastOrNull { it.headlineViewEvent.articleId == recommendationsItem.articleId }
                ?.let { item ->
                    item.startTime?.let { startTime ->
                        if (item.headlineViewEvent.readingTimeMilli == null) {
                            item.headlineViewEvent.readingTimeMilli =
                                System.currentTimeMillis() - startTime
                            userHistoryPrefRepo.writeHeadlineViewItemToStorage(item.headlineViewEvent)
                            Logger.d(
                                TAG,
                                "headline_view (auto_recirc) stop timer\narticleId: ${recommendationsItem.articleId}\nreadingTimeSec: ${item.headlineViewEvent.readingTimeMilli}",
                            )
                        }
                    }
                }
        }
    }

    /** Compose overload — no carouselId, uses a default bucket in the map. */
    private fun startAutoRecircViewTimer(
        recommendationsItems: List<AutoRecircHelperItem>,
        adapterPosition: Int,
    ) = startAutoRecircViewTimer(COMPOSE_DEFAULT_CAROUSEL_ID, recommendationsItems, adapterPosition)

    /** Compose overload — no carouselId, uses a default bucket in the map. */
    private fun stopAutoRecircViewTimer(
        recommendationsItems: List<AutoRecircHelperItem>,
        adapterPosition: Int,
    ) = stopAutoRecircViewTimer(COMPOSE_DEFAULT_CAROUSEL_ID, recommendationsItems, adapterPosition)

    fun captureAutoRecircViewAction(
        action: HeadlineViewAction,
        carouselId: String,
        recommendationsItem: AutoRecircHelperItem,
        adapterPosition: Int,
    ) {
        autoRecircViewedItemsWithStartTimesByCarouselId
            .getOrPut(carouselId, { mutableListOf() })
            .lastOrNull { it.headlineViewEvent.articleId == recommendationsItem.articleId }
            ?.let {
                setAutoRecircAction(it.headlineViewEvent, action)
            } ?: run {
            addAutoRecircView(carouselId, recommendationsItem, adapterPosition, false, action)
        }
        Logger.d(
            TAG,
            "headline_view (auto_recirc) action\n" +
                    "articleId: ${recommendationsItem.articleId}\n" +
                    "action: $action",
        )
    }

    private fun addAutoRecircView(
        carouselId: String,
        recommendationsItem: AutoRecircHelperItem,
        adapterPosition: Int,
        shouldStartTimer: Boolean,
        action: HeadlineViewAction? = null,
    ) {
        val jucId = userHistoryManager.getJucId()
        logJucIdBreadcrumbIfNeeded(jucId, "DefaultHeadlineViewEvent")
        val event = DefaultHeadlineViewEvent(
            clientEventTime = userHistoryManager.getClientEventTime(),
            articleId = recommendationsItem.articleId,
            wapoLoginId = userHistoryManager.getWapoLoginId(),
            jucId = jucId,
            jtid = userHistoryManager.getJtId(),
            surface = SURFACE_RECIRC,
            surfaceVariant = getSurfaceVariant(),
            recommendationContentType = RECOMMENDATION_CONTENT_TYPE_ARTICLE,
            requestId = recommendationsItem.requestId,
            currentUrl = getPath(recommendationsItem.currentUrl),
            moduleCategory = recommendationsItem.collectionCategory,
            modulePosition = adapterPosition + 1,
            positionInModule = recommendationsItem.positionInModule + 1,
            pageViewId = pageViewId,
        )

        setAutoRecircAction(event, action)

        val helperItem =
            if (shouldStartTimer) {
                HeadlineViewHelperItem(
                    event,
                    adapterPosition,
                    System.currentTimeMillis()
                )
            } else {
                HeadlineViewHelperItem(event, adapterPosition)
            }

        autoRecircViewedItemsWithStartTimesByCarouselId
            .getOrPut(carouselId, { mutableListOf() })
            .add(helperItem)
        Logger.d(
            TAG,
            "headline_view (auto_recirc) event_created\n" +
                    "carouselId: $carouselId\n" +
                    "articleId: ${recommendationsItem.articleId}\n" +
                    "requestId: ${recommendationsItem.requestId}\n" +
                    "timer started: $shouldStartTimer\n" +
                    "action: $action"
        )
    }

    private fun setAutoRecircAction(
        headlineViewItem: DefaultHeadlineViewEvent,
        action: HeadlineViewAction?,
    ) {
        when (action) {
            HeadlineViewAction.CLICKED -> {
                headlineViewItem.apply { clicked = true }
            }

            HeadlineViewAction.LISTENED -> {
                headlineViewItem.apply { listened = true }
            }

            HeadlineViewAction.SHARED -> {
                headlineViewItem.apply { shared = true }
            }

            HeadlineViewAction.ADDED_PLAYLIST -> {
                headlineViewItem.apply { addedPlaylist = true }
            }

            HeadlineViewAction.SAVED_STORY -> {
                headlineViewItem.apply { savedStory = true }
            }

            HeadlineViewAction.GIFTED -> {
                headlineViewItem.apply { gifted = true }
            }

            else -> { /* No action */
            }
        }
    }

    //  endregion auto_recirculation

    //region habit_tile_viewed
    fun addHabitTileViewedItem(
        tileLink: String?,
        requestId: String?,
        position: Int?,
        tileCategory: String?,
        tileLabel: String?,
        tileCategoryDetail: String? = null,
        testGroup: String?,
    ) {

        val habitTileViewedItem =
            HabitTileViewItem(
                tileLink = tileLink,
                wapoLoginId = userHistoryManager.getWapoLoginId(),
                jucId = userHistoryManager.getJucId(),
                requestId = requestId,
                surface = SURFACE_HOMEPAGE,
                position = position ?: -1,
                tileCategory = tileCategory,
                tileLabel = tileLabel,
                tileCategoryDetail = tileCategoryDetail,
                clientEventTime = userHistoryManager.getClientEventTime(),
                testGroup = testGroup
            )

        userHistoryPrefRepo.writeHabitTileViewedItemToStorage(habitTileViewedItem)
    }

    fun addClickedToHabitTileViewedItem(tileLink: String?) {
        userHistoryPrefRepo.addClickedToHabitTileViewedItem(tileLink)
    }
    //endregion habit_tile_viewed

    //region pageview
    fun savePageViewEvent(
        articleId: String?,
        contentType: String?,
    ) {
        articleId ?: return
        contentType ?: return
        val jucId = userHistoryManager.getJucId()
        val consentGiven = userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven
        //  Log only if jucId is null
        if (jucId.isNullOrEmpty() && consentGiven) {
            val eventLogBuilder = EventLog
                .Builder()
                .setMessage("Breadcrumb - j_ucid=${jucId} at savePageViewEvent")
                .set("consentGiven", consentGiven)
                .setModule(LogModules.FOR_YOU)
            remoteLog.e(eventLogBuilder.build())
        }
        val pageViewItem =
            PageViewItem(
                articleId,
                wapoLoginId = userHistoryManager.getWapoLoginId(),
                jucId = jucId,
                pageViewId = pageViewId,
                jtId = userHistoryManager.getJtId(),
                contentType = contentType,
                clientEventTime = userHistoryManager.getClientEventTime(),
            )
        userHistoryPrefRepo.writePageViewItemsToStorage(setOf(pageViewItem))
    }
    //endregion pageview

    private fun logJucIdBreadcrumbIfNeeded(jucId: String?, location: String) {
        val consentGiven = userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven
        val isOnline = AppContextUtils.isConnectingOrConnected()
        if (jucId.isNullOrEmpty() && isOnline && consentGiven) {
            remoteLog.e(
                EventLog.Builder()
                    .setMessage("Breadcrumb - j_ucid=${jucId} at $location")
                    .set("consentGiven", consentGiven)
                    .setModule(LogModules.FOR_YOU)
                    .build()
            )
        }
    }

    private fun getSurfaceVariant(): String =
        if (deviceUtilRepo.isTablet()) {
            "tablet"
        } else {
            "phone"
        }

    private fun getPath(url: String): String {
        val urlParser = URLParser(url)
        return if (urlParser.getDomain().isEmpty()) url else urlParser.getPath()
    }

    fun updateFYSessionId() {
        forYouFeedSessionId = UUID.randomUUID().toString()
    }

    companion object {
        private val TAG = UserHistoryViewModel::class.java.simpleName
        val INCLUDED_CONTENT_TYPES = listOf("sanitized_html", "list")

        //        const val NOT_FOUND_CLIENT = "not_found_client"
        const val SURFACE_FEED = "feed"
        const val SURFACE_RECIRC = "recirc"
        const val SURFACE_HOMEPAGE = "homepage"
        const val RECOMMENDATION_CONTENT_TYPE_ARTICLE = "article"

        /** Default carousel bucket used by Compose callers that don't have a carouselId. */
        private const val COMPOSE_DEFAULT_CAROUSEL_ID = "compose_default"
    }
}
