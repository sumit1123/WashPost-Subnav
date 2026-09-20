package com.wapo.flagship.features.userhistory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.util.CoroutinesTestRule
import com.washingtonpost.userhistory.ForYouViewedAction
import com.washingtonpost.userhistory.domain.UserHistoryManager
import com.washingtonpost.userhistory.domain.UserHistoryPrefRepo
import com.washingtonpost.userhistory.models.HabitTileViewItem
import com.washingtonpost.userhistory.models.PageViewItem
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.washingtonpost.userhistory.models.ScrollDepthItem
import com.washingtonpost.userhistory.models.UserHistoryArticleItem
import com.washingtonpost.userhistory.models.VideoConclusionState
import com.washingtonpost.userhistory.repo.UserHistoryMetaData
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class UserHistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var viewModel: UserHistoryViewModel
    private lateinit var userHistoryManager: UserHistoryManager
    private lateinit var userHistoryPrefRepo: UserHistoryPrefRepo
    private lateinit var remoteLog: RemoteLogRepo
    private lateinit var deviceUtilRepo: DeviceUtilRepo
    private lateinit var userHistoryMetaData: UserHistoryMetaData

    @Before
    fun setUp() {
        mockkStatic(Logger::class)
        every { Logger.d(any(), any()) } just Runs

        userHistoryManager = mockk(relaxed = true)
        userHistoryPrefRepo = mockk(relaxed = true)
        remoteLog = mockk(relaxed = true)
        deviceUtilRepo = mockk(relaxed = true)

        userHistoryMetaData = mockk(relaxed = true)
        every { userHistoryMetaData.privacyConsentGiven } returns true
        every { userHistoryPrefRepo.getUserHistoryMeta() } returns userHistoryMetaData

        viewModel = UserHistoryViewModel(userHistoryManager, userHistoryPrefRepo, remoteLog, deviceUtilRepo)
    }

    @Test
    fun `Given user history events, when postUserHistoryEvents is called, then manager is invoked`() = runTest {
        // When
        viewModel.postUserHistoryEvents()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { userHistoryManager.postUserHistoryEvents() }
    }

    @Test
    fun `Given a page view event, when postPageViewEvent is called, then manager is invoked`() = runTest {
        // When
        viewModel.postPageViewEvent()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { userHistoryManager.postPageViewEvent() }
    }

    @Test
    fun `Given push event data, when postPushEvent is called, then manager is invoked`() = runTest {
        // Given
        val url = "https://www.washingtonpost.com/some-article"
        val pushId = "push-123"

        // When
        viewModel.postPushEvent(url, pushId, "alert", "A", true, "login")
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { userHistoryManager.postPushEvent(url, pushId, "alert", "A", true, "login") }
    }

    @Test
    fun `Given video viewed data, when writeVideoViewedEvent is called, then manager is invoked`() {
        // Given
        val videoId = "video-789"
        val conclusionState = VideoConclusionState.VIDEO_END

        // When
        viewModel.writeVideoViewedEvent(videoId, "Politics", 15000L, 30000L, 60L, conclusionState)

        // Then
        verify(exactly = 1) { userHistoryManager.writeVideoViewedEvent(videoId, "Politics", 15000L, 30000L, 60L, conclusionState) }
    }

    @Test
    fun `Given valid inputs, when initializeScrollDepth is called, then scroll tracking is set up and logged correctly`() {
        // Given
        val articleId = "article-101"
        val deepestScrollId = "item-2"
        val items = listOf(
            UserHistoryArticleItem("item-1", "sanitized_html"),
            UserHistoryArticleItem("item-2", "list"),
            UserHistoryArticleItem("item-3", "other"), // This one should be filtered out
        )
        val scrollDepthSlot = slot<ScrollDepthItem>()
        val eventLogSlot = slot<EventLog>()
        val loggerSlot = slot<String>()
        every { userHistoryPrefRepo.writeScrollDepthEventToStorage(capture(scrollDepthSlot)) } just Runs
        every { remoteLog.e(capture(eventLogSlot)) } just Runs
        every { Logger.d(any(), capture(loggerSlot)) } just Runs
        every { userHistoryManager.getJucId() } returns null // To trigger the breadcrumb log

        // When
        viewModel.initializeScrollDepth(articleId, items, deepestScrollId)

        // Then
        val capturedItem = scrollDepthSlot.captured
        assertEquals(articleId, capturedItem.articleId)
        assertEquals(2, capturedItem.totalElements)
        assertEquals(1, capturedItem.deepestScrollIndex)
        assertEquals(deepestScrollId, capturedItem.deepestScrollId)

        assertEquals(true, eventLogSlot.captured.dataString.contains("Breadcrumb - j_ucid=null at ScrollDepth"))

        val capturedLog = loggerSlot.captured
        assertTrue(capturedLog.contains("initializeScrollDepth"))
        assertTrue(capturedLog.contains("articleId: $articleId"))
        assertTrue(capturedLog.contains("total_elements: 2"))
    }

    @Test
    fun `Given a deeper scroll, when updateScrollDepth is called, then it logs the update`() {
        // Given
        val articleId = "article-102"
        val items = listOf(
            UserHistoryArticleItem("item-1", "sanitized_html"),
            UserHistoryArticleItem("item-2", "list")
        )
        viewModel.initializeScrollDepth(articleId, items, "item-1")
        val loggerSlot = slot<String>()
        every { Logger.d(any(), capture(loggerSlot)) } just Runs

        // When
        viewModel.updateScrollDepth("item-2")

        // Then
        val capturedLog = loggerSlot.captured
        assertTrue(capturedLog.contains("updateScrollDepth"))
        assertTrue(capturedLog.contains("most_recent_scroll_index: 2"))
        assertTrue(capturedLog.contains("deepest_scroll_index: 2"))
        assertTrue(capturedLog.contains("deepest_scroll_id: item-2"))
    }

    @Test
    fun `Given an initialized scroll depth, when finalizeScrollDepthItem is called, then repository is updated correctly`() {
        // Given
        val articleId = "article-103"
        val items = listOf(UserHistoryArticleItem("item-1", "sanitized_html"))
        viewModel.initializeScrollDepth(articleId, items, null)
        val startTime = System.currentTimeMillis() - 5000 // 5 seconds ago
        val pageViewIdSlot = slot<String>()
        val indexSlot = slot<Int>()
        val idSlot = slot<String>()
        every { userHistoryPrefRepo.updateScrollDepth(capture(pageViewIdSlot), capture(indexSlot), capture(idSlot), any(), any()) } just Runs

        // When
        viewModel.finalizeScrollDepthItem(startTime)

        // Then
        assertEquals(0, indexSlot.captured)
        assertEquals("item-1", idSlot.captured)
    }

    @Test
    fun `Given a user action, when captureForYouViewAction is called, then event is written and logged`() {
        // Given
        val recommendation = RecommendationsHelperItem("article-104", "req-1", "recipe-1", "test-A", "rec-reason", "type")
        val loggerSlot = slot<String>()
        every { Logger.d(any(), capture(loggerSlot)) } just Runs

        // When
        viewModel.captureForYouViewAction(ForYouViewedAction.CLICKED, recommendation, 1, "feed")

        // Then
        val capturedLog = loggerSlot.captured
        assertTrue(capturedLog.contains("fy_viewed action"))
        assertTrue(capturedLog.contains("articleId: article-104"))
        assertTrue(capturedLog.contains("action: CLICKED"))
    }

    @Test
    fun `Given push listener data, when writePushListenerEvent is called, then manager is invoked`() {
        // Given
        val url = "https://www.washingtonpost.com/some-other-article"

        // When
        viewModel.writePushListenerEvent(url, "push-456", "breaking", "B", "user-789", false)

        // Then
        verify(exactly = 1) { userHistoryManager.writePushListenerEvent(url, "push-456", "breaking", "B", "user-789", false) }
    }

    @Test
    fun `Given habit tile data, when addHabitTileViewedItem is called, then event is written to repo`() {
        // Given
        val tileLink = "/some-link"
        val itemSlot = slot<HabitTileViewItem>()
        every { userHistoryPrefRepo.writeHabitTileViewedItemToStorage(capture(itemSlot)) } just Runs

        // When
        viewModel.addHabitTileViewedItem(tileLink, "req-2", 2, "category", "label", "detail", "C")

        // Then
        val capturedItem = itemSlot.captured
        assertEquals(tileLink, capturedItem.tileLink)
        assertEquals("req-2", capturedItem.requestId)
        assertEquals(2, capturedItem.position)
    }

    @Test
    fun `Given a clicked habit tile, when addClickedToHabitTileViewedItem is called, then repo is updated`() {
        // Given
        val tileLink = "/some-link-2"

        // When
        viewModel.addClickedToHabitTileViewedItem(tileLink)

        // Then
        verify(exactly = 1) { userHistoryPrefRepo.addClickedToHabitTileViewedItem(tileLink) }
    }

    @Test
    fun `Given page view data, when savePageViewEvent is called, then event is written and logged if needed`() {
        // Given
        val articleId = "article-105"
        val itemSlot = slot<Set<PageViewItem>>()
        val eventLogSlot = slot<EventLog>()
        every { userHistoryPrefRepo.writePageViewItemsToStorage(capture(itemSlot)) } just Runs
        every { remoteLog.e(capture(eventLogSlot)) } just Runs
        every { userHistoryManager.getJucId() } returns null // To trigger the breadcrumb log

        // When
        viewModel.savePageViewEvent(articleId, "article")

        // Then
        val capturedItem = itemSlot.captured.first()
        assertEquals(articleId, capturedItem.articleId)
        assertEquals("article", capturedItem.contentType)

        assertEquals(true, eventLogSlot.captured.dataString.contains("Breadcrumb - j_ucid=null at savePageViewEvent"))
    }
}
