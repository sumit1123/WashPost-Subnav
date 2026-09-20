package com.washingtonpost.userhistory.repo

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.userhistory.domain.UserHistoryPrefRepo
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
import com.washingtonpost.userhistory.remote.POST_USER_HISTORY_EVENT_TYPE
import com.washingtonpost.userhistory.remote.USER_HISTORY_EVENT_TYPE
import com.washingtonpost.userhistory.remote.UserHistoryEventType
import com.washingtonpost.userhistory.remote.UserHistoryService
import jakarta.inject.Inject
import java.lang.reflect.Type

class UserHistoryPrefRepoImpl @Inject constructor(
    private var userHistoryPreferences: SharedPreferences,
    private val userHistoryService: UserHistoryService,
    private val userHistoryMetaProvider: UserHistoryMetaProvider,
) :
    UserHistoryPrefRepo {

    override fun writeScrollDepthEventToStorage(event: ScrollDepthItem) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readScrollDepthEventsFromStorage() ?: mutableSetOf()
        allEvents.add(event)
        userHistoryPreferences.edit {
            this.putString(SCROLL_DEPTH_EVENTS, Gson().toJson(allEvents))
        }
    }

    override fun updateScrollDepth(
        pageViewId: String,
        deepestScrollIndex: Int,
        deepestScrollId: String,
        readingTimeMilli: Long,
        clientEventTime: String
    ) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readScrollDepthEventsFromStorage()
        allEvents?.firstOrNull {
            it.pageViewId == pageViewId
        }?.apply {
            this.deepestScrollIndex = deepestScrollIndex
            this.deepestScrollId = deepestScrollId
            this.readingTimeMilli = readingTimeMilli
            this.clientEventTime = clientEventTime
        }
        userHistoryPreferences.edit {
            this.putString(SCROLL_DEPTH_EVENTS, Gson().toJson(allEvents))
        }
    }

    override fun writeContentListenEventToStorage(event: ContentListenItem) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readContentListenEventsFromStorage() ?: mutableSetOf()
        allEvents.add(event)
        userHistoryPreferences.edit {
            this.putString(CONTENT_LISTEN_EVENTS, Gson().toJson(allEvents))
        }
    }

    override fun writeForYouViewedItemToStorage(event: ForYouViewedItem) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readForYouViewedEventsFromStorage() ?: mutableSetOf()
        allEvents.add(event)
        userHistoryPreferences.edit {
            this.putString(FOR_YOU_VIEWED_EVENTS, Gson().toJson(allEvents))
        }
    }

    override fun writeHeadlineViewItemToStorage(event: DefaultHeadlineViewEvent) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readHeadlineViewEventsFromStorage() ?: mutableSetOf()
        allEvents.add(event)
        userHistoryPreferences.edit {
            this.putString(HEADLINE_VIEW_EVENTS, Gson().toJson(allEvents))
        }
    }

    override fun writeHabitTileViewedItemToStorage(event: HabitTileViewItem) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readHabitTileViewEventsFromStorage() ?: mutableSetOf()
        allEvents.add(event)
        userHistoryPreferences.edit {
            this.putString(HABIT_TILE_VIEW_EVENTS, Gson().toJson(allEvents))
        }
    }

    override fun addClickedToHabitTileViewedItem(tileLink: String?) {
        tileLink ?: return
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val events = readHabitTileViewEventsFromStorage()
        events?.firstOrNull { it.tileLink == tileLink }?.clicked = true
        userHistoryPreferences.edit {
            this.putString(HABIT_TILE_VIEW_EVENTS, Gson().toJson(events))
        }
    }

    override fun writePageViewItemsToStorage(events: Set<PageViewItem>) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readPageViewEventsFromStorage() ?: mutableSetOf()
        allEvents.addAll(events)
        userHistoryPreferences.edit {
            this.putString(PAGE_VIEW_EVENTS, Gson().toJson(allEvents))
        }
    }

    /**
     * Persists a single push notification event into local storage.
     * This function retrieves the existing stored push events, adds the new one to the set,
     * and writes the updated set back to SharedPreferences as a JSON string.
     */
    override fun writePushEventsToStorage(events: PushNotificationViewItem) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readPushEventsFromStorage() ?: mutableSetOf()
        allEvents.add(events)
        try {
            userHistoryPreferences.edit {
                this.putString(PUSH_ORIGINATED_EVENTS, Gson().toJson(allEvents))
            }
        } catch (e: Exception) {
            val builder: EventLog.Builder = EventLog.Builder()
            builder
                .setMessage("writing push events to storage failed")
                .setModule(LogModules.FOR_YOU)
                .setErrorMessage(e.message)
                .set("cause", e.cause)
            RemoteLog.e(AppContextUtils.appContext, builder.build())
        }
    }

    override fun writeVideoEventsToStorage(events: VideoViewItem) {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) return
        val allEvents = readVideoEventsFromStorage() ?: mutableSetOf()
        allEvents.add(events)
        userHistoryPreferences.edit {
            this.putString(VIDEO_VIEWED_EVENTS, Gson().toJson(allEvents))
        }
    }

    override fun clearPageViewEvents() {
        userHistoryPreferences.edit {
            this.putString(PAGE_VIEW_EVENTS, null)
        }
    }

    override fun clearPushEvents() {
        userHistoryPreferences.edit {
            this.putString(PUSH_ORIGINATED_EVENTS, null)
        }
    }

    override fun clearAllEvents() {
        userHistoryPreferences.edit {
            this.putString(SCROLL_DEPTH_EVENTS, null)
            this.putString(CONTENT_LISTEN_EVENTS, null)
            this.putString(FOR_YOU_VIEWED_EVENTS, null)
            this.putString(HABIT_TILE_VIEW_EVENTS, null)
            this.putString(PAGE_VIEW_EVENTS, null)
            this.putString(PUSH_ORIGINATED_EVENTS, null)
            this.putString(VIDEO_VIEWED_EVENTS, null)
            this.putString(HEADLINE_VIEW_EVENTS, null)
        }
    }

    override fun writeEventsToDeadLetter(events: Set<UserHistoryBaseEvent>) {
        val allEvents = readEventsFromStorage().toMutableSet()
        allEvents.addAll(events)

        if (allEvents.size > DEAD_LETTER_CAP) {
            allEvents.removeAll(allEvents.take(allEvents.size - DEAD_LETTER_CAP).toSet())
        }
        userHistoryPreferences.edit {
            this.putString(DEAD_LETTER_EVENTS, Gson().toJson(allEvents))
        }
    }

    /** Can be used in the future */
    override fun clearDeadLetter() {
        userHistoryPreferences.edit {
            this.putString(DEAD_LETTER_EVENTS, null)
        }
    }

    override fun readEventsFromStorage(): Set<UserHistoryBaseEvent> {
        val events = mutableSetOf<UserHistoryBaseEvent>()

        readScrollDepthEventsFromStorage()?.let {
            events.addAll(it)
        }

        readContentListenEventsFromStorage()?.let {
            events.addAll(it)
        }

        readForYouViewedEventsFromStorage()?.let {
            events.addAll(it)
        }

        readHeadlineViewEventsFromStorage()?.let {
            events.addAll(it)
        }

        readHabitTileViewEventsFromStorage()?.let {
            events.addAll(it)
        }

        readPageViewEventsFromStorage()?.let {
            events.addAll(it)
        }

        readPushEventsFromStorage()?.let {
            events.addAll(it)
        }

        readVideoEventsFromStorage()?.let {
            events.addAll(it)
        }

        val sortedEvents = events.sortedBy { it.clientEventTime }

        val newestEvents = if (sortedEvents.size > REQUEST_CAP) {
            val difference = sortedEvents.size - REQUEST_CAP
            sortedEvents.subList(difference, sortedEvents.size)
        } else {
            sortedEvents
        }

        return newestEvents.toSet()
    }

    private fun readScrollDepthEventsFromStorage(): MutableSet<ScrollDepthItem>? {
        val data = userHistoryPreferences.getString(SCROLL_DEPTH_EVENTS, null)
        val type: Type = object : TypeToken<Set<ScrollDepthItem>?>() {}.type
        return try {
            Gson().fromJson(data, type)
        } catch (_: Exception) {
            null
        }
    }

    private fun readContentListenEventsFromStorage(): MutableSet<ContentListenItem>? {
        val data = userHistoryPreferences.getString(CONTENT_LISTEN_EVENTS, null)
        val type: Type = object : TypeToken<Set<ContentListenItem>?>() {}.type
        return try {
            Gson().fromJson(data, type)
        } catch (_: Exception) {
            null
        }
    }

    private fun readForYouViewedEventsFromStorage(): MutableSet<ForYouViewedItem>? {
        val data = userHistoryPreferences.getString(FOR_YOU_VIEWED_EVENTS, null)
        val type: Type = object : TypeToken<Set<ForYouViewedItem>?>() {}.type
        return try {
            Gson().fromJson(data, type)
        } catch (_: Exception) {
            null
        }
    }

    private fun readHeadlineViewEventsFromStorage(): MutableSet<DefaultHeadlineViewEvent>? {
        val data = userHistoryPreferences.getString(HEADLINE_VIEW_EVENTS, null)
        val type: Type = object : TypeToken<Set<DefaultHeadlineViewEvent>?>() {}.type
        return try {
            Gson().fromJson(data, type)
        } catch (_: Exception) {
            null
        }
    }

    private fun readHabitTileViewEventsFromStorage(): MutableSet<HabitTileViewItem>? {
        val data = userHistoryPreferences.getString(HABIT_TILE_VIEW_EVENTS, null)
        val type: Type = object : TypeToken<Set<HabitTileViewItem>?>() {}.type
        return try {
            Gson().fromJson(data, type)
        } catch (_: Exception) {
            null
        }
    }

    override fun readPageViewEventsFromStorage(): MutableSet<PageViewItem>? {
        val data = userHistoryPreferences.getString(PAGE_VIEW_EVENTS, null)
        val type: Type = object : TypeToken<Set<PageViewItem>?>() {}.type
        return try {
            Gson().fromJson(data, type)
        } catch (_: Exception) {
            null
        }
    }

    override fun readPushEventsFromStorage(): MutableSet<PushNotificationViewItem>? {
        val data = userHistoryPreferences.getString(PUSH_ORIGINATED_EVENTS, null)
        val type: Type = object : TypeToken<Set<PushNotificationViewItem>?>() {}.type
        return try {
            Gson().fromJson(data, type)
        } catch (_: Exception) {
            null
        }
    }

    override fun readVideoEventsFromStorage(): MutableSet<VideoViewItem>? {
        val data = userHistoryPreferences.getString(VIDEO_VIEWED_EVENTS, null)
        val type: Type = object : TypeToken<Set<VideoViewItem>?>() {}.type
        return try {
            Gson().fromJson(data, type)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun postUserHistoryEvents(
        event: UserHistoryEventType,
        userHistoryEvents: UserHistoryEvents
    ): APIResult<UserHistoryServicePostResponse>? {
        if (!userHistoryMetaProvider.getUserHistoryMeta().privacyConsentGiven) {
            Logger.d(TAG, "Privacy consent not given. Clearing stored events and not posting.")
            return null
        }
        var loggableEvents = ""
        userHistoryEvents.events.forEach { loggableEvents += "$it\n" }
        Logger.d(TAG, "${POST_USER_HISTORY_EVENT_TYPE}\nevents:\n$loggableEvents")
        val apiResult = userHistoryService.postUserHistoryEvents(getHeaders(), userHistoryEvents)
        logApiResult(apiResult, event)
        return apiResult
    }

    override fun getUserHistoryMeta(): UserHistoryMetaData {
        return userHistoryMetaProvider.getUserHistoryMeta()
    }

    private fun logApiResult(
        apiResult: APIResult<UserHistoryServicePostResponse>,
        event: UserHistoryEventType
    ) {
        when (apiResult) {
            is APIResult.Failure -> {
                val isOnline = AppContextUtils.isConnectingOrConnected()
                if (isOnline) {
                    val eventLogBuilder =
                        EventLog
                            .Builder()
                            .setMessage("$POST_USER_HISTORY_EVENT_TYPE FAILURE")
                            .setErrorCode(apiResult.statusCode)
                            .set(USER_HISTORY_EVENT_TYPE, event.name)
                            .set("response", apiResult.rawResponse)
                            .setModule(LogModules.FOR_YOU)
                    RemoteLog.e(AppContextUtils.appContext, eventLogBuilder.build())
                }
            }

            is APIResult.NetworkError -> Unit

            is APIResult.Success -> Unit
        }
    }

    private fun getHeaders(): HashMap<String, String> {
        val headers: HashMap<String, String> = HashMap()
        headers["Content-Type"] = "application/json"
        return headers
    }

    companion object {
        private const val TAG = "UserHistoryPrefHelper"
        private const val REQUEST_CAP = 500

        private const val DEAD_LETTER_CAP = 1000

        const val USER_HISTORY_PREFS_NAME = "user_history_prefs"

        private const val SCROLL_DEPTH_EVENTS = "scroll_depth_events"

        private const val CONTENT_LISTEN_EVENTS = "content_listen_events"

        private const val FOR_YOU_VIEWED_EVENTS = "for_you_viewed_events"

        private const val HEADLINE_VIEW_EVENTS = "headling_view_events"

        private const val HABIT_TILE_VIEW_EVENTS = "habit_tile_view_events"

        private const val PAGE_VIEW_EVENTS = "page_view_events"

        private const val PUSH_ORIGINATED_EVENTS = "push_originated_events"

        private const val VIDEO_VIEWED_EVENTS = "video_view_event"

        private const val DEAD_LETTER_EVENTS = "dead_letter_events"
    }
}
