/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.sections

import com.wapo.flagship.data.repository.TestData
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.sections.domein.SectionRibbonRepo
import com.wapo.flagship.features.sections.model.Section
import com.wapo.flagship.features.sections.model.SectionType
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonEvents
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonUiState
import com.wapo.flagship.features.sections.viewmodels.sectionsribbon.SectionsRibbonViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SectionsRibbonViewModelTest : BaseViewModelTest<SectionsRibbonUiState, SectionsRibbonEvents?>() {

    private lateinit var sectionRibbonRepo: SectionRibbonRepo
    private lateinit var viewModel: SectionsRibbonViewModel

    @Before
    fun setup() {
        sectionRibbonRepo = mockk(relaxed = true)
        viewModel = SectionsRibbonViewModel(sectionRibbonRepo)
    }

    override fun collectUIStates(): StateFlow<SectionsRibbonUiState> {
        return viewModel.uiState
    }

    override fun collectEvents(): SharedFlow<SectionsRibbonEvents?> {
        return viewModel.sectionsRibbonEvent
    }

    @Test
    fun `Given a new list of sections, when setSections is called, then uiState is updated`() = runTest {
        viewModelTest_runTest {
            // When
            viewModel.setSections(TestData.Section.testSections)
            advanceUntilIdle()

            // Then
            val lastState = uiStates.last()
            assertEquals(TestData.Section.testSections, lastState.sections)
            assertTrue(lastState.isVisible)
        }
    }

    @Test
    fun `Given tracking data, when sendTrackingData is called, then SendTrackingInfoEvent is emitted`() = runTest {
        val tracking = mockk<Tracking>()
        viewModelTest_runTest {
            // When
            viewModel.sendTrackingData(tracking)
            advanceUntilIdle()

            // Then
            val lastEvent = events.last()
            assertTrue(lastEvent is SectionsRibbonEvents.SendTrackingInfoEvent)
            assertEquals(tracking, (lastEvent as SectionsRibbonEvents.SendTrackingInfoEvent).tracking)
        }
    }

    @Test
    fun `Given last viewed section is 'For You', when initSelectedSection is called, then For You tab is selected`() = runTest {
        viewModelTest_runTest {
            // Given
            every { sectionRibbonRepo.getLastViewed() } returns "for-you"

            // When
            viewModel.setSections(TestData.Section.testSections)
            advanceUntilIdle()

            // Then
            val lastEvent = events.firstOrNull {
                it is SectionsRibbonEvents.OpenSectionOnLaunchEvent
            }
            assertTrue(lastEvent is SectionsRibbonEvents.OpenSectionOnLaunchEvent)
            assertTrue((lastEvent as SectionsRibbonEvents.OpenSectionOnLaunchEvent).open)
            assertEquals(3, uiStates.last().selectedSectionIndex)
        }
    }

    @Test
    fun `Given a valid index, when setSelectedSectionIndex is called, then uiState and repo are updated`() = runTest {
        viewModelTest_runTest {
            // Given
            viewModel.setSections(TestData.Section.testSections)

            // When
            viewModel.setSelectedSectionIndex(1)
            advanceUntilIdle()

            // Then
            assertEquals(1, uiStates.last().selectedSectionIndex)
            verify { sectionRibbonRepo.setLastViewed("2") }
        }
    }

    @Test
    fun `When removeLastViewedSection is called, then repo is updated`() {
        // When
        viewModel.removeLastViewedSection()

        // Then
        verify { sectionRibbonRepo.removeLastViewed() }
    }

    @Test
    fun `When setRibbonReady is called, then RibbonReadyEvent is emitted`() = runTest {
        viewModelTest_runTest {
            // When
            viewModel.setRibbonReady(true)
            advanceUntilIdle()

            // Then
            assertTrue(events.last() is SectionsRibbonEvents.RibbonReadyEvent)
            assertTrue(viewModel.isRibbonReady())
        }
    }

    @Test
    fun `When checkNewsprintVisited is called on a newsprint section, then VisitedNewsprintSectionEvent is emitted`() = runTest {
        viewModelTest_runTest {
            // Given
            val newsprintSections = TestData.Section.testSections + Section("newsprint-1", "/newsprint", "Print", "Print", sectionType = SectionType.SECTION)
            viewModel.setSections(newsprintSections)
            viewModel.setSelectedSectionIndex(4)

            // When
            viewModel.checkNewsprintVisited()
            advanceUntilIdle()

            // Then
            val lastEvent = events.last()
            assertTrue(lastEvent is SectionsRibbonEvents.VisitedNewsprintSectionEvent)
            assertTrue((lastEvent as SectionsRibbonEvents.VisitedNewsprintSectionEvent).visited)
            assertTrue(uiStates.last().visitedNewsprintSection)
        }
    }

    @Test
    fun `When saveCurrentSection is called, then last viewed section is saved in repo`() {
        // Given
        viewModel.setSections(TestData.Section.testSections)
        viewModel.setSelectedSectionIndex(2)

        // When
        viewModel.saveCurrentSection()

        // Then
        verify { sectionRibbonRepo.setLastViewed("3") }
    }

    @Test
    fun `When appOpenedOnSection is called with a sectionId, then opened on key is set in repo`() {
        // Given
        val sectionId = "test-section"

        // When
        viewModel.appOpenedOnSection(sectionId)

        // Then
        verify { sectionRibbonRepo.setOpenedOnKey(sectionId) }
    }



    @Test
    fun `When appOpenedOnSection is called with null, then opened on key is removed and event is emitted`() = runTest {
        viewModelTest_runTest {
            // When
            viewModel.appOpenedOnSection(null)
            advanceUntilIdle()

            // Then
            verify { sectionRibbonRepo.removeOpenedOnKey() }
            val lastEvent = events.last()
            assertTrue(lastEvent is SectionsRibbonEvents.OpenSectionOnLaunchEvent)
            assertFalse((lastEvent as SectionsRibbonEvents.OpenSectionOnLaunchEvent).open)
        }
    }
}
