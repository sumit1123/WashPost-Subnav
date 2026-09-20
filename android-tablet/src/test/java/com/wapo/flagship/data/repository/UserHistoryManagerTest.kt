package com.wapo.flagship.data.repository

import android.net.Uri
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.data.repository.TestData.UserHistory.createDummyScrollDepthItem
import com.washingtonpost.userhistory.domain.UserHistoryPrefRepo
import com.washingtonpost.userhistory.models.PushNotificationViewItem
import com.washingtonpost.userhistory.models.UserHistoryEvents
import com.washingtonpost.userhistory.network.APIResult
import com.washingtonpost.userhistory.remote.UserHistoryEventType
import com.washingtonpost.userhistory.repo.UserHistoryManagerImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class UserHistoryManagerTest {

    private lateinit var userHistoryPrefRepo: UserHistoryPrefRepo
    private lateinit var remoteLogRepo: RemoteLogRepo
    private lateinit var deviceUtilRepo: DeviceUtilRepo
    private lateinit var userHistoryManager: UserHistoryManagerImpl

    @Before
    fun setUp() {
        mockkStatic(Logger::class)
        every { Logger.d(any(), any()) } just Runs

        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>(relaxed = true)
        every { Uri.parse(any()) } returns mockUri
        every { mockUri.path } returns "/path/to/article"

        remoteLogRepo = mockk(relaxed = true)
        userHistoryPrefRepo = mockk(relaxed = true)
        deviceUtilRepo = mockk(relaxed = true)

        userHistoryManager = UserHistoryManagerImpl(Dispatchers.Unconfined, userHistoryPrefRepo, remoteLogRepo, deviceUtilRepo)
    }

    @Test
    fun `Given events in repo, when postUserHistoryEvents is called with success, then events are cleared`() = runTest {
        // Given
        val events = setOf(createDummyScrollDepthItem())
        every { userHistoryPrefRepo.readEventsFromStorage() } returns events
        coEvery { userHistoryPrefRepo.postUserHistoryEvents(any(), any()) } returns APIResult.Success(mockk())

        // When
        userHistoryManager.postUserHistoryEvents()

        // Then
        coVerify { userHistoryPrefRepo.postUserHistoryEvents(UserHistoryEventType.ALL, any()) }
        verify { userHistoryPrefRepo.clearAllEvents() }
        verify(exactly = 0) { userHistoryPrefRepo.writeEventsToDeadLetter(any()) }
    }

    @Test
    fun `Given events in repo, when postUserHistoryEvents is called with 4xx error, then events are cleared and sent to dead letter`() = runTest {
        // Given
        val events = setOf(createDummyScrollDepthItem())
        every { userHistoryPrefRepo.readEventsFromStorage() } returns events
        coEvery { userHistoryPrefRepo.postUserHistoryEvents(any(), any()) } returns APIResult.Failure(400, null)

        // When
        userHistoryManager.postUserHistoryEvents()

        // Then
        verify { userHistoryPrefRepo.clearAllEvents() }
        verify { userHistoryPrefRepo.writeEventsToDeadLetter(events) }
    }

    @Test
    fun `Given events in repo, when postUserHistoryEvents is called with 5xx error, then events are not cleared`() = runTest {
        // Given
        val events = setOf(createDummyScrollDepthItem())
        every { userHistoryPrefRepo.readEventsFromStorage() } returns events
        coEvery { userHistoryPrefRepo.postUserHistoryEvents(any(), any()) } returns APIResult.Failure(500, null)

        // When
        userHistoryManager.postUserHistoryEvents()

        // Then
        verify(exactly = 0) { userHistoryPrefRepo.clearAllEvents() }
        verify(exactly = 0) { userHistoryPrefRepo.writeEventsToDeadLetter(any()) }
    }

    @Test
    fun `Given a push event, when postPushEvent is called, then it is constructed and posted correctly`() = runTest {
        // Given
        val url = "https://www.washingtonpost.com/path/to/article"
        val pushId = "push-123"
        val pushType = "alert"
        val testGroup = "B"
        val isPushClicked = true
        val loginId = "user-abc"
        val eventsSlot = slot<UserHistoryEvents>()
        every { userHistoryPrefRepo.readPushEventsFromStorage() } returns mutableSetOf(mockk(relaxed = true))
        coEvery { userHistoryPrefRepo.postUserHistoryEvents(eq(UserHistoryEventType.PUSH_ORIGINATED), capture(eventsSlot)) } returns APIResult.Success(mockk())
        every { deviceUtilRepo.isTablet() } returns false

        // When
        userHistoryManager.postPushEvent(url, pushId, pushType, testGroup, isPushClicked, loginId)

        // Then
        val capturedEvent = eventsSlot.captured.events.first() as PushNotificationViewItem
        assertEquals("/path/to/article", capturedEvent.canonicalUrl)
        assertEquals(pushId, capturedEvent.pushId)
        assertEquals(pushType, capturedEvent.pushCategory)
        assertEquals(testGroup, capturedEvent.testGroup)
        assertEquals(isPushClicked, capturedEvent.clicked)
        assertEquals(loginId, capturedEvent.loginId)
        assertEquals("phone", capturedEvent.surfaceVariant)
    }

    @Test
    fun `Given a null jucId and consent, when postPushEvent is called, then a breadcrumb is logged`() = runTest {
        // Given
        every { userHistoryPrefRepo.getUserHistoryMeta().jucId } returns null
        every { userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven } returns true
        every { userHistoryPrefRepo.readPushEventsFromStorage() } returns mutableSetOf(mockk(relaxed = true))
        val logSlot = slot<EventLog>()
        every { remoteLogRepo.e(capture(logSlot)) } just Runs

        // When
        userHistoryManager.postPushEvent("url", null, "", "", false, null)

        // Then
        assertTrue(logSlot.captured.dataString.contains("message=\"Breadcrumb – j_ucid is null in postPushEvent\""))
    }
}
