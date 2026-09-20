package com.wapo.flagship.features.sections

import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.features.sections.repo.SectionTrackingRepository
import com.wapo.flagship.features.sections.tracking.SectionTrackEvent
import com.wapo.flagship.features.sections.viewmodels.SectionNavigation
import com.wapo.flagship.features.sections.viewmodels.SectionTrackingViewModel
import com.washingtonpost.userhistory.domain.UserHistoryManager
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SectionTrackingViewModelTest : BaseViewModelTest<Unit, SectionTrackEvent>() {
    private lateinit var userHistoryManager: UserHistoryManager
    private lateinit var sectionTrackingRepository: SectionTrackingRepository
    private lateinit var viewModel: SectionTrackingViewModel

    @Before
    fun setup() {
        userHistoryManager = mockk<UserHistoryManager>(relaxed = true)
        sectionTrackingRepository = SectionTrackingRepository()
        viewModel = SectionTrackingViewModel(userHistoryManager, sectionTrackingRepository)
    }

    override fun collectUIStates(): StateFlow<Unit> = MutableStateFlow(Unit)

    override fun collectEvents(): SharedFlow<SectionTrackEvent> = viewModel.event

    @Test
    fun sectionTrackingViewModel_trackPageView_Success_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val event = SectionTrackEvent.PageView("Home", mockk(relaxed = true))

            viewModelTest_runTest {
                viewModel.trackEvent(event, pos = 0)
                advanceUntilIdle()

                assertTrue(events.contains(event))
            }
        }

    @Test
    fun sectionTrackingViewModel_trackPageView_Debounce_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val event = SectionTrackEvent.PageView("Home", mockk(relaxed = true))

            viewModelTest_runTest {
                viewModel.trackEvent(event, pos = 0)
                viewModel.trackEvent(event, pos = 0)

                advanceUntilIdle()

                assertEquals(1, events.size)
            }
        }

    @Test
    fun sectionTrackingViewModel_trackPageView_DuringRefresh_Ignored_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val event = SectionTrackEvent.PageView("Home", mockk(relaxed = true))

            viewModelTest_runTest {
                viewModel.setNavigating(SectionNavigation.REFRESH)
                viewModel.trackEvent(event, pos = 0)
                advanceUntilIdle()

                assertTrue(events.isEmpty())
            }
        }

    @Test
    fun sectionTrackingViewModel_onPageTap_Newsprint_SetsMiscellany_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val event = SectionTrackEvent.OnpageTap(tracking = mockk(relaxed = true))

            viewModelTest_runTest {
                viewModel.trackEvent(event, pos = 0, from = SectionTrackingViewModel.NEWSPRINT_TOP_CARD)
                advanceUntilIdle()

                assertTrue(events.contains(event))
                assertEquals("sf_newsprint_type1", viewModel.getMiscellany())
            }
        }


    @Test
    fun sectionTrackingViewModel_NavigationBehavior_RibbonTap_Mapping_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val event = SectionTrackEvent.PageView("News", mockk(relaxed = true))

            viewModelTest_runTest {
                viewModel.setNavigating(SectionNavigation.RIBBON_TAP)
                viewModel.trackEvent(event, pos = 4)
                advanceUntilIdle()

                assertEquals("top_ribbon_5", viewModel.getNavigationBehavior())
            }
        }

    @Test
    fun sectionTrackingViewModel_trackPageView_SwipeNavigation_SyncsHistory_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val event = SectionTrackEvent.PageView("News", mockk(relaxed = true))
            val position = 4

            viewModelTest_runTest {
                viewModel.setNavigating(SectionNavigation.SWIPE)
                viewModel.trackEvent(event, pos = position)
                advanceUntilIdle()

                coVerify { userHistoryManager.postUserHistoryEvents() }
                assertEquals("swipe_5", viewModel.getNavigationBehavior())

            }
        }

    @Test
    fun sectionTrackingViewModel_trackAudioStart_EmitsImmediately_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val event = SectionTrackEvent.AudioStart(
                avName = "Podcast",
                touchpoint = "Section",
                miscellany = null,
                itemType = "audio",
                id = "123"
            )

            viewModelTest_runTest {
                viewModel.trackEvent(event)
                advanceUntilIdle()

                assertTrue(events.contains(event))
            }
        }

    @Test
    fun sectionTrackingViewModel_trackAudioInteraction_EmitsImmediately_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val event = SectionTrackEvent.AudioInteraction(
                avName = "Podcast",
                touchpoint = "Section",
                miscellany = "test",
                itemType = "audio"
            )

            viewModelTest_runTest {
                viewModel.trackEvent(event)
                advanceUntilIdle()

                assertTrue(events.contains(event))
                assertEquals(1, events.size)
            }
        }
}