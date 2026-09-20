package com.wapo.flagship.data.repository

import com.wapo.flagship.features.aixp.domain.PostAnswersAIRepo
import com.wapo.flagship.features.aixp.models.AskThePostShare
import com.wapo.flagship.features.aixp.models.AskThePostShareTurnRequest
import com.wapo.flagship.features.aixp.models.ConversationTurn
import com.wapo.flagship.features.aixp.models.HistoryResponse
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackResponse
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.ask.repo.AskThePostRepo
import com.wapo.flagship.features.ask.repo.AskThePostRepoImpl
import com.wapo.flagship.util.CoroutinesTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Call

@OptIn(ExperimentalCoroutinesApi::class)
class AskThePostRepoTest {

    private lateinit var askThePostRepo: AskThePostRepo
    private lateinit var postAnswersAIRepo: PostAnswersAIRepo

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()


    @Before
    fun setup() {

        postAnswersAIRepo = mockk(relaxed = true)
        // The repo can now be instantiated without crashing.
        askThePostRepo = AskThePostRepoImpl(postAnswersAIRepo)
    }

    @Test
    fun `getSessionHistory returns success when service is successful`() = runTest {
        // Given
        val uuid = "test-uuid"
        val expectedResponse = listOf(HistoryResponse("conv1", "title1", "123L", "", emptyList()))
        val successResult = APIResult.Success(expectedResponse)
        coEvery { postAnswersAIRepo.getSessionHistory(uuid) } returns successResult

        // When
        val result = askThePostRepo.getSessionHistory(uuid)

        // Then
        coVerify(exactly = 1) { postAnswersAIRepo.getSessionHistory(uuid) }
        assertEquals(successResult, result)
    }

    @Test
    fun `getSessionHistory returns error when service fails`() = runTest {
        // Given
        val uuid = "test-uuid"
        val errorResult = APIResult.Failure(500, "Internal Server Error")
        coEvery { postAnswersAIRepo.getSessionHistory(uuid) } returns errorResult

        // When
        val result = askThePostRepo.getSessionHistory(uuid)

        // Then
        coVerify(exactly = 1) { postAnswersAIRepo.getSessionHistory(uuid) }
        assertTrue(result is APIResult.Failure)
        assertEquals(500, (result as APIResult.Failure).statusCode)
    }

    @Test
    fun `getConversationHistory returns success when service is successful`() = runTest {
        // Given
        val conversationId = "conv-123"
        val uuid = "user-abc"
        val expectedResponse =
            listOf(
                ConversationTurn(
                    123,
                    timestamp = "",
                    inResponseTo = 456,
                    role = "Hello",
                    message = "message"
                )
            )
        val successResult = APIResult.Success(expectedResponse)
        coEvery {
            postAnswersAIRepo.getConversationHistory(
                conversationId,
                uuid
            )
        } returns successResult

        // When
        val result = askThePostRepo.getConversationHistory(conversationId, uuid)

        // Then
        coVerify(exactly = 1) {
            postAnswersAIRepo.getConversationHistory(
                conversationId,
                uuid
            )
        }
        assertEquals(successResult, result)
    }

    @Test
    fun `deleteConversation returns success when service is successful`() = runTest {
        // Given
        val conversationId = "conv-123"
        val uuid = "user-abc"
        val expectedResponse = PostAnswersFeedbackResponse("Success")
        val successResult = APIResult.Success(expectedResponse)
        coEvery {
            postAnswersAIRepo.deleteConversation(
                conversationId,
                uuid
            )
        } returns successResult

        // When
        val result = askThePostRepo.deleteConversation(conversationId, uuid)

        // Then
        coVerify(exactly = 1) {
            postAnswersAIRepo.deleteConversation(
                conversationId,
                uuid
            )
        }
        assertEquals(successResult, result)
    }

    @Test
    fun `shareChat returns success with shareId when service is successful`() = runTest {
        // Given
        val conversationId = "conv-123"
        val request = AskThePostShareTurnRequest(conversationId)
        val expectedShareId = "shared-xyz"
        val successResult = APIResult.Success(expectedShareId)
        coEvery {
            postAnswersAIRepo.shareChat(
                conversationId,
                request
            )
        } returns successResult

        // When
        val result = askThePostRepo.shareChat(conversationId, request)

        // Then
        coVerify(exactly = 1) {
            postAnswersAIRepo.shareChat(
                conversationId,
                request
            )
        }
        assertEquals(successResult, result)
    }

    @Test
    fun `getSharedConversation returns success when service is successful`() = runTest {
        // Given
        val shareId = "shared-xyz"
        val expectedResponse = HistoryResponse("conv1", "Shared Chat", "", "", emptyList())
        val successResult = APIResult.Success(expectedResponse)
        coEvery {
            postAnswersAIRepo.getSharedConversation(
                shareId
            )
        } returns successResult

        // When
        val result = askThePostRepo.getSharedConversation(shareId)

        // Then
        coVerify(exactly = 1) { postAnswersAIRepo.getSharedConversation(shareId) }
        assertEquals(successResult, result)
    }

    @Test
    fun `deleteShareLink calls service method`() {
        // Given
        val shareId = "shared-xyz"
        val mockCall: Call<Void> = mockk(relaxed = true)
        every { postAnswersAIRepo.deleteShareLink(shareId) } returns mockCall

        // When
        val result = askThePostRepo.deleteShareLink(shareId)

        // Then
        verify(exactly = 1) { postAnswersAIRepo.deleteShareLink(shareId) }
        assertEquals(mockCall, result)
    }

    @Test
    fun `deleteAllShareLinks calls service method`() {
        // Given
        val mockCall: Call<Void> = mockk(relaxed = true)
        every { postAnswersAIRepo.deleteAllShareLinks() } returns mockCall

        // When
        val result = askThePostRepo.deleteAllShareLinks()

        // Then
        verify(exactly = 1) { postAnswersAIRepo.deleteAllShareLinks() }
        assertEquals(mockCall, result)
    }

    @Test
    fun `getShares returns success when service is successful`() = runTest {
        // Given
        val successResult = APIResult.Success(List(1) { AskThePostShare("share1", "Shared Title 1", "456L", "") })
        coEvery { postAnswersAIRepo.getShares() } returns successResult

        // When
        val result = askThePostRepo.getShares()

        // Then
        coVerify(exactly = 1) { postAnswersAIRepo.getShares() }
        assertEquals(successResult, result)
    }
}
