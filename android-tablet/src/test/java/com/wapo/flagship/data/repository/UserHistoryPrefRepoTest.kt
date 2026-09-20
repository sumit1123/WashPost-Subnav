package com.wapo.flagship.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.data.repository.TestData.UserHistory.createDummyScrollDepthItem
import com.wapo.flagship.data.repository.TestData.UserHistory.pushNotificationViewItem
import com.washingtonpost.userhistory.models.PushNotificationViewItem
import com.washingtonpost.userhistory.models.ScrollDepthItem
import com.washingtonpost.userhistory.models.UserHistoryBaseEvent
import com.washingtonpost.userhistory.models.UserHistoryEvents
import com.washingtonpost.userhistory.network.APIResult
import com.washingtonpost.userhistory.remote.UserHistoryEventType
import com.washingtonpost.userhistory.remote.UserHistoryService
import com.washingtonpost.userhistory.repo.UserHistoryMetaData
import com.washingtonpost.userhistory.repo.UserHistoryMetaProvider
import com.washingtonpost.userhistory.repo.UserHistoryPrefRepoImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserHistoryPrefRepoTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var userHistoryService: UserHistoryService
    private lateinit var userHistoryMetaProvider: UserHistoryMetaProvider
    private lateinit var userHistoryPrefRepo: UserHistoryPrefRepoImpl

    private val gson = Gson()

    @Before
    fun setUp() {
        // Mock static loggers
        mockkStatic(Logger::class)
        mockkStatic(RemoteLog::class)
        mockkObject(AppContextUtils)
        every { Logger.d(any(), any()) } just Runs
        every { Logger.e(any(), any()) } just Runs
        every { RemoteLog.e(any(), any()) } just Runs
        every { RemoteLog.d(any(), any()) } just Runs
        every { AppContextUtils.isConnectingOrConnected() } returns true

        // Mock dependencies
        sharedPreferences = mockk(relaxed = true)
        sharedPreferencesEditor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        userHistoryService = mockk(relaxed = true)
        userHistoryMetaProvider = mockk(relaxed = true)
        every { userHistoryMetaProvider.getUserHistoryMeta() } returns UserHistoryMetaData(
            loginId = null,
            jucId = null,
            jtId = null,
            clientId = null,
            deviceId = null,
            appVersion = null,
            platform = null,
            privacyConsentGiven = true
        )

        userHistoryPrefRepo = UserHistoryPrefRepoImpl(sharedPreferences, userHistoryService, userHistoryMetaProvider)
    }

    @Test
    fun `Given a scroll depth event, when writeScrollDepthEventToStorage is called, then it is serialized and saved to prefs`() {
        // Given
        val event = createDummyScrollDepthItem()
        val keySlot = slot<String>()
        val valueSlot = slot<String>()
        every { sharedPreferencesEditor.putString(capture(keySlot), capture(valueSlot)) } returns sharedPreferencesEditor

        // When
        userHistoryPrefRepo.writeScrollDepthEventToStorage(event)

        // Then
        verify { sharedPreferencesEditor.apply() }
        assertEquals("scroll_depth_events", keySlot.captured)
        val capturedJson = valueSlot.captured
        assertTrue(capturedJson.contains(""""articleid":"article1""""))
        assertTrue(capturedJson.contains(""""pageview_id":"pv1""""))
    }

    @Test
    fun `Given an existing event, when updateScrollDepth is called, then the correct event is updated in prefs`() {
        // Given
        val initialEvent1 = createDummyScrollDepthItem(pageViewId = "pv1", deepestScrollIndex = 1)
        val initialEvent2 = createDummyScrollDepthItem(pageViewId = "pv2", deepestScrollIndex = 5)
        val initialJson = gson.toJson(setOf(initialEvent1, initialEvent2))
        every { sharedPreferences.getString("scroll_depth_events", null) } returns initialJson
        val valueSlot = slot<String>()
        every { sharedPreferencesEditor.putString(any(), capture(valueSlot)) } returns sharedPreferencesEditor

        // When
        userHistoryPrefRepo.updateScrollDepth("pv1", 2, "id2", 5000, "time")

        // Then
        verify { sharedPreferencesEditor.apply() }
        val updatedEvents = gson.fromJson(valueSlot.captured, Array<ScrollDepthItem>::class.java)
        val updatedEvent = updatedEvents.find { it.pageViewId == "pv1" }
        val untouchedEvent = updatedEvents.find { it.pageViewId == "pv2" }

        assertEquals(2, updatedEvent?.deepestScrollIndex)
        assertEquals("id2", updatedEvent?.deepestScrollId)
        assertEquals(5, untouchedEvent?.deepestScrollIndex) // Ensure other items are not modified
    }

    @Test
    fun `When clearAllEvents is called, then all event keys are set to null in prefs`() {
        // When
        userHistoryPrefRepo.clearAllEvents()

        // Then
        verify { sharedPreferencesEditor.putString("scroll_depth_events", null) }
        verify { sharedPreferencesEditor.putString("content_listen_events", null) }
        verify { sharedPreferencesEditor.putString("for_you_viewed_events", null) }
        verify { sharedPreferencesEditor.putString("habit_tile_view_events", null) }
        verify { sharedPreferencesEditor.putString("page_view_events", null) }
        verify { sharedPreferencesEditor.putString("push_originated_events", null) }
        verify { sharedPreferencesEditor.putString("video_view_event", null) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `When writeEventsToDeadLetter is called, then events are saved to the dead letter key`() {
        // Given
        val events = setOf<UserHistoryBaseEvent>(createDummyScrollDepthItem(articleId = "dead_event"))
        val keySlot = slot<String>()
        val valueSlot = slot<String>()
        every { sharedPreferencesEditor.putString(capture(keySlot), capture(valueSlot)) } returns sharedPreferencesEditor

        // When
        userHistoryPrefRepo.writeEventsToDeadLetter(events)

        // Then
        verify { sharedPreferencesEditor.apply() }
        assertEquals("dead_letter_events", keySlot.captured)
        assertTrue(valueSlot.captured.contains("dead_event"))
    }

    @Test
    fun `When readEventsFromStorage is called, then it reads and combines events from all sources`() {
        // Given
        val scrollEvent = createDummyScrollDepthItem(articleId = "scroll_event")
        every { sharedPreferences.getString("scroll_depth_events", null) } returns gson.toJson(setOf(scrollEvent))
        every { sharedPreferences.getString("push_originated_events", null) } returns gson.toJson(setOf(pushNotificationViewItem))

        // When
        val events = userHistoryPrefRepo.readEventsFromStorage()

        // Then
        assertEquals(2, events.size)
        assertTrue(events.any { it is ScrollDepthItem && it.articleId == "scroll_event" })
        assertTrue(events.any { it is PushNotificationViewItem && it.articleId == "push_event" })
    }

    @Test
    fun `Given an exception during write, when writePushEventsToStorage is called, then RemoteLog captures the error`() {
        // Given
        val exception = RuntimeException("Disk is full")
        every { sharedPreferencesEditor.putString(any(), any()) } throws exception
        val logSlot = slot<EventLog>()
        every { RemoteLog.e(any(), capture(logSlot)) } just Runs

        // When
        userHistoryPrefRepo.writePushEventsToStorage(mockk(relaxed = true))

        // Then
        val capturedLog = logSlot.captured
        assertTrue(capturedLog.dataString.contains("message=\"writing push events to storage failed\""))
    }

    @Test
    fun `Given APIResult Failure and device is online, when postUserHistoryEvents is called, then RemoteLog logs the error`() = runTest {
        // Given
        val failure = APIResult.Failure(500, "Internal Server Error")
        coEvery { userHistoryService.postUserHistoryEvents(any(), any()) } returns failure
        every { AppContextUtils.isConnectingOrConnected() } returns true
        val logSlot = slot<EventLog>()
        every { RemoteLog.e(any(), capture(logSlot)) } just Runs

        // When
        userHistoryPrefRepo.postUserHistoryEvents(UserHistoryEventType.ALL, UserHistoryEvents(emptyList()))

        // Then
        val capturedLog = logSlot.captured
        assertTrue(capturedLog.dataString.contains("postUserHistoryEvents FAILURE"))
    }

    @Test
    fun `Given APIResult Failure and device is offline, when postUserHistoryEvents is called, then RemoteLog is not called`() = runTest {
        // Given
        val failure = APIResult.Failure(500, "Internal Server Error")
        coEvery { userHistoryService.postUserHistoryEvents(any(), any()) } returns failure
        every { AppContextUtils.isConnectingOrConnected() } returns false

        // When
        userHistoryPrefRepo.postUserHistoryEvents(UserHistoryEventType.ALL, UserHistoryEvents(emptyList()))

        // Then
        verify(exactly = 0) { RemoteLog.e(any(), any()) }
    }

    @Test
    fun `Given APIResult NetworkError, when postUserHistoryEvents is called, then RemoteLog is not called`() = runTest {
        // Given
        val networkError = APIResult.NetworkError(RuntimeException("No network"))
        coEvery { userHistoryService.postUserHistoryEvents(any(), any()) } returns networkError

        // When
        userHistoryPrefRepo.postUserHistoryEvents(UserHistoryEventType.ALL, UserHistoryEvents(emptyList()))

        // Then
        verify(exactly = 0) { RemoteLog.e(any(), any()) }
    }

    @Test
    fun `Given APIResult Success, when postUserHistoryEvents is called, then RemoteLog is not called`() = runTest {
        // Given
        val success = APIResult.Success(data = null)
        coEvery { userHistoryService.postUserHistoryEvents(any(), any()) } returns success

        // When
        userHistoryPrefRepo.postUserHistoryEvents(UserHistoryEventType.ALL, UserHistoryEvents(emptyList()))

        // Then
        verify(exactly = 0) { RemoteLog.e(any(), any()) }
    }
}
