package com.wapo.flagship.features.personalizedpodcasts.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.personalizedpodcasts.repo.PersonalizedPodcastRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
class PersonalizedPodcastViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PersonalizedPodcastViewModel
    private val personalizedPodcastRepository: PersonalizedPodcastRepository = mockk()
    private val audioManager: ClassicAudioManager2 = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PersonalizedPodcastViewModel(
            context = mockk(relaxed = true),
            personalizedPodcastRepository = personalizedPodcastRepository,
            audioManager = audioManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getListOfPodcasts updates uiState with podcasts`() = runTest {
        val podcasts = listOf(
            PersonalizedPodcast(id = "1", title = "Podcast 1", mp3Url = "", itemType = "", kicker = "", transcript = "", summary = "", audioFilePath = "", audioDuration = 0f, totalCharacters = 0f, image = "", articlesUsed = emptyList(), createdAt = ""),
            PersonalizedPodcast(id = "2", title = "Podcast 2", mp3Url = "", itemType = "", kicker = "", transcript = "", summary = "", audioFilePath = "", audioDuration = 0f, totalCharacters = 0f, image = "", articlesUsed = emptyList(), createdAt = "")
        )
        coEvery { personalizedPodcastRepository.getPodcasts() } returns podcasts

        viewModel.getListOfPodcasts()
        advanceUntilIdle()

        assertEquals(podcasts, viewModel.uiState.value.personalizedPodcasts)
    }
}