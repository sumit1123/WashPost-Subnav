package com.wapo.flagship.features.personalizedpodcasts.repo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.personalizedpodcasts.model.PodcastConfigResponse
import com.wapo.flagship.features.personalizedpodcasts.service.PersonalizedPodcastService
import com.washingtonpost.userhistory.network.APIResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PersonalizedPodcastRepositoryImplTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: PersonalizedPodcastRepositoryImpl
    private val personalizedPodcastService: PersonalizedPodcastService = mockk()
    private val audioProvider: AudioProvider = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = PersonalizedPodcastRepositoryImpl(
            context = mockk(relaxed = true),
            personalizedPodcastService = personalizedPodcastService,
            audioProvider = audioProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getPodcastConfigs returns configs on success`() = runTest {
        val podcastConfigResponse = PodcastConfigResponse(null, emptyList(), null)
        coEvery { personalizedPodcastService.getPodcastConfigs(any()) } returns APIResult.Success(podcastConfigResponse)

        val result = repository.getPodcastConfigs()

        assertEquals(podcastConfigResponse, result)
    }
}
