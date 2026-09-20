package com.wapo.flagship.features.backendhealthchecker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.domain.repository.HealthStatusRepo
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.features.backendhealth.models.Article
import com.wapo.flagship.features.backendhealth.models.BackendHealthStatus
import com.wapo.flagship.features.backendhealth.models.FailoverPageResponse
import com.wapo.flagship.features.backendhealth.models.FailoverState
import com.wapo.flagship.features.backendhealth.repository.FailoverRepository
import com.wapo.flagship.features.backendhealth.viewmodels.BackendHealthCheckerViewModel
import com.wapo.flagship.network.retrofit.network.APIResult
import com.washingtonpost.android.config.domain.models.config.BackendHealthConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackendHealthCheckerViewModelTest : BaseViewModelTest<FailoverState, Unit>() {

    private lateinit var healthStatusRepo: HealthStatusRepo
    private lateinit var remoteLogRepo: RemoteLogRepo
    private lateinit var failoverRepository: FailoverRepository
    private lateinit var viewModel: BackendHealthCheckerViewModel

    private val backendHealthConfig = BackendHealthConfig(
        backendHealthMonitorURL = "https://health.com",
        fallbackURL = "https://fallback.com",
        fallbackStaticURL = "https://fallback-static.com"
    )

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(coroutinesTestRule.dispatcher)

        healthStatusRepo = mockk(relaxed = true)
        remoteLogRepo = mockk(relaxed = true)
        failoverRepository = mockk(relaxed = true)

        viewModel = BackendHealthCheckerViewModel(
            coroutinesTestRule.dispatcher,
            healthStatusRepo,
            remoteLogRepo,
            failoverRepository
        )
    }

    override fun collectUIStates(): StateFlow<FailoverState> {
        return viewModel.uiState
    }

    override fun collectEvents(): SharedFlow<Unit> = MutableStateFlow(Unit)

    @Test
    fun `Given backend is healthy, when checkBackendHealth, then state is updated to healthy`() = runTest {
        viewModelTest_runTest {
            // Given
            val healthyStatus = BackendHealthStatus(isHealthy = true)
            coEvery { healthStatusRepo.fetchHealthStatus(any(), any(), any()) } returns healthyStatus
            coEvery { healthStatusRepo.isFailoverActive() } returns true // Assume it was active before

            // When
            viewModel.checkBackendHealth(backendHealthConfig)
            advanceUntilIdle()

            // Then
            val lastState = uiStates.last()
            assertTrue(lastState.isHealthy)
            assertFalse(lastState.showFallbackPopUp)
            coVerify { remoteLogRepo.e(any()) } // Verify status is logged
        }
    }

    @Test
    fun `Given backend is unhealthy and failover succeeds, when checkBackendHealth, then state is updated with articles`() = runTest {
        viewModelTest_runTest {
            // Given
            val unhealthyStatus = BackendHealthStatus(isHealthy = false, fallbackStaticURL = "url")
            val failoverArticle = Article("headline", "byline", "thumb", "url")
            val failoverPage = FailoverPageResponse(listOf(failoverArticle))
            coEvery { healthStatusRepo.fetchHealthStatus(any(), any(), any()) } returns unhealthyStatus
            coEvery { failoverRepository.fetchArticles(any()) } returns APIResult.Success(failoverPage, Headers.headersOf())
            coEvery { healthStatusRepo.isFailoverActive() } returns false

            // When
            viewModel.checkBackendHealth(backendHealthConfig)
            advanceUntilIdle()

            // Then
            val lastState = uiStates.last()
            assertFalse(lastState.isHealthy)
            assertTrue(lastState.showFallbackPopUp)
            assertEquals(1, lastState.articles.size)
            coVerify { remoteLogRepo.e(any()) } // Verify status is logged
        }
    }

    @Test
    fun `Given backend is unhealthy and failover fails, when checkBackendHealth, then state is updated with empty articles`() = runTest {
        viewModelTest_runTest {
            // Given
            val unhealthyStatus = BackendHealthStatus(isHealthy = false, fallbackStaticURL = "url")
            coEvery { healthStatusRepo.fetchHealthStatus(any(), any(), any()) } returns unhealthyStatus
            coEvery { failoverRepository.fetchArticles(any()) } returns APIResult.Failure(500, null)

            // When
            viewModel.checkBackendHealth(backendHealthConfig)
            advanceUntilIdle()

            // Then
            val lastState = uiStates.last()
            assertFalse(lastState.isHealthy)
            assertTrue(lastState.showFallbackPopUp)
            assertTrue(lastState.articles.isEmpty())
        }
    }

    @Test
    fun `Given backend is unhealthy but has no fallback URL, when checkBackendHealth, then popup is not shown`() = runTest {
        viewModelTest_runTest {
            // Given
            val unhealthyStatus = BackendHealthStatus(isHealthy = false, fallbackURL = null, fallbackStaticURL = null)
            coEvery { healthStatusRepo.fetchHealthStatus(any(), any(), any()) } returns unhealthyStatus

            // When
            viewModel.checkBackendHealth(backendHealthConfig)
            advanceUntilIdle()

            // Then
            val lastState = uiStates.last()
            assertFalse(lastState.isHealthy)
            assertFalse(lastState.showFallbackPopUp)
        }
    }
}
