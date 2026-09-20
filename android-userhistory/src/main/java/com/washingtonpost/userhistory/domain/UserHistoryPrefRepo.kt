package com.washingtonpost.userhistory.domain

import com.washingtonpost.userhistory.models.ContentListenItem
import com.washingtonpost.userhistory.models.DefaultHeadlineViewEvent
import com.washingtonpost.userhistory.models.ForYouViewedItem
import com.washingtonpost.userhistory.models.HabitTileViewItem
import com.washingtonpost.userhistory.models.PageViewItem
import com.washingtonpost.userhistory.models.PushNotificationViewItem
import com.washingtonpost.userhistory.models.ScrollDepthItem
import com.washingtonpost.userhistory.models.UserHistoryBaseEvent
import com.washingtonpost.userhistory.models.UserHistoryEvents
import com.washingtonpost.userhistory.models.UserHistoryServicePostResponse
import com.washingtonpost.userhistory.models.VideoViewItem
import com.washingtonpost.userhistory.network.APIResult
import com.washingtonpost.userhistory.remote.UserHistoryEventType
import com.washingtonpost.userhistory.repo.UserHistoryMetaData

interface UserHistoryPrefRepo {
    fun writeScrollDepthEventToStorage(event: ScrollDepthItem)
    fun updateScrollDepth(
        pageViewId: String,
        deepestScrollIndex: Int,
        deepestScrollId: String,
        readingTimeMilli: Long,
        clientEventTime: String
    )
    fun writeContentListenEventToStorage(event: ContentListenItem)
    fun writeForYouViewedItemToStorage(event: ForYouViewedItem)
    fun writeHeadlineViewItemToStorage(event: DefaultHeadlineViewEvent)
    fun writeHabitTileViewedItemToStorage(event: HabitTileViewItem)
    fun addClickedToHabitTileViewedItem(tileLink: String?)
    fun writePageViewItemsToStorage(events: Set<PageViewItem>)
    fun writePushEventsToStorage(events: PushNotificationViewItem)
    fun writeVideoEventsToStorage(events: VideoViewItem)
    fun clearPageViewEvents()
    fun clearPushEvents()
    fun clearAllEvents()
    fun writeEventsToDeadLetter(events: Set<UserHistoryBaseEvent>)
    fun clearDeadLetter()
    fun readEventsFromStorage(): Set<UserHistoryBaseEvent>
    fun readPageViewEventsFromStorage(): MutableSet<PageViewItem>?
    fun readPushEventsFromStorage(): MutableSet<PushNotificationViewItem>?
    fun readVideoEventsFromStorage(): MutableSet<VideoViewItem>?

    suspend fun postUserHistoryEvents(
        event: UserHistoryEventType,
        userHistoryEvents: UserHistoryEvents
    ): APIResult<UserHistoryServicePostResponse>?

    fun getUserHistoryMeta(): UserHistoryMetaData
}