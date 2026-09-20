package com.wapo.flagship.features.ask


import android.content.Context
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.domain.repository.SearchRepo
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.ask.repo.AskThePostRepo
import com.wapo.flagship.features.ask.viewmodels.AskThePostViewModel
import com.wapo.flagship.features.ask.viewmodels.ShareUrlState
import com.wapo.flagship.features.search2.events.ConversationItem
import com.wapo.flagship.features.search2.events.SseEvent
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.AppContext
import com.wapo.flagship.features.ask.models.AskThePostEvent
import com.wapo.flagship.features.ask.models.AskThePostUIState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AskThePostViewModelTest : BaseViewModelTest<AskThePostUIState, AskThePostEvent>() {

    private lateinit var viewModel: AskThePostViewModel
    private lateinit var searchRepo: SearchRepo
    private lateinit var askThePostRepo: AskThePostRepo
    private lateinit var mockContext: Context
    private lateinit var remoteLog: RemoteLogRepo
    private lateinit var loadRenderMetrics: LoadRenderMetrics


    private val sseEventStateFlow = MutableStateFlow<SseEvent>(SseEvent.ForceStop)

    @Before
    fun setup() {
        // Mock all static dependencies
        mockkStatic(
            Logger::class,
            AppContext::class,
            Measurement::class
        )
        every { Logger.d(any(), any()) } just Runs
        every { Logger.e(any(), any(), any<Throwable>()) } just Runs
        every { Logger.e(any(), any()) } just Runs
        every { AppContext.getAirshipNamedUserId(any()) } returns "test-user"
        every { Measurement.trackEnteredAskQuestionEvent(any(), any(), any()) } just Runs

        // Mock repository dependencies
        searchRepo = mockk(relaxed = true)
        askThePostRepo = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        remoteLog = mockk(relaxed = true)
        loadRenderMetrics = mockk(relaxed = true)

        // Stub the SSE event flow from the repository
        every { searchRepo.sseEventState } returns sseEventStateFlow

        // Initialize the ViewModel with mocked dependencies
        viewModel = AskThePostViewModel(mockContext, searchRepo, askThePostRepo, remoteLog, loadRenderMetrics)
    }

    override fun collectUIStates(): StateFlow<AskThePostUIState> {
        return viewModel.uiState
    }

    override fun collectEvents(): SharedFlow<AskThePostEvent> {
        return viewModel.askThePostEvent
    }

    @Test
    fun `viewModel init should set initial uiState correctly`() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                advanceUntilIdle()

                val initialState = uiStates.first()
                assertFalse(
                    "History categories should be populated",
                    initialState.historyCategories.isEmpty()
                )
                assertTrue(
                    "Conversation history should be empty",
                    initialState.conversationHistory.isEmpty()
                )
                assertEquals(ShareUrlState.Empty, initialState.shareUrlState)
            }
        }

    @Test
    fun `startLiveConversation should update uiState`() =
        runTest(coroutinesTestRule.dispatcher) {
            val query = "What is the latest news?"
            viewModelTest_runTest {
                viewModel.startLiveConversation(query)
                advanceUntilIdle()

                val lastState = uiStates.last()
                assertEquals(1, lastState.conversationHistory.size)
                val questionItem =
                    lastState.conversationHistory.first() as ConversationItem.QuestionItem
                assertEquals(query, questionItem.text)

                verify { Measurement.trackEnteredAskQuestionEvent(query, any(), any()) }
            }
        }

    @Test
    fun `sseEvent Error should update uiState with ErrorItem`() =
        runTest(coroutinesTestRule.dispatcher) {
            val loggerSlot = slot<String>()
            every { Logger.e(any(), capture(loggerSlot), any()) } just Runs

            viewModelTest_runTest {
                viewModel.startLiveConversation("What is the latest news?")
                advanceUntilIdle()
                // Trigger an error from the SSE flow
                sseEventStateFlow.value =
                    SseEvent.Error(Throwable(message = "SSE Error: 404"), null)
                advanceUntilIdle()

                val lastState = uiStates.last()
                assertTrue(lastState.conversationHistory.last() is ConversationItem.ErrorItem)
                assertFalse(lastState.isStreamingResponseLoading)
            }
        }

    @Test
    fun `shareATP failure should update uiState to Error`() =
        runTest(coroutinesTestRule.dispatcher) {
            val conversationId = "conv-123"
            coEvery { askThePostRepo.shareChat(any(), any()) } returns APIResult.Failure(
                500,
                "Server Error"
            )

            viewModelTest_runTest {
                // First, establish a conversation to share
                viewModel.startLiveConversation("test question")
                sseEventStateFlow.value =
                    SseEvent.Data(conversationId, conversationId, "NEW_CONVERSATION_ID")
                advanceUntilIdle()

                // Now, attempt to share
                viewModel.shareATP(isTurn = false)
                advanceUntilIdle()

                val lastState = uiStates.last()
                assertTrue(
                    "ShareUrlState should be Error",
                    lastState.shareUrlState is ShareUrlState.Error
                )
            }
        }
}
