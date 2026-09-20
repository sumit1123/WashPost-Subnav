package com.wapo.flagship.features.sections

import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.features.grid.domain.model.SectionHabitTile
import com.wapo.flagship.features.grid.domain.model.SectionHabitTilesData
import com.wapo.flagship.features.grid.domain.repository.SectionHabitTilesRepository
import com.wapo.flagship.features.grid.viewmodel.sectionsHabitTiles.SectionsHabitTilesViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SectionsHabitTilesViewModelTest : BaseViewModelTest<SectionHabitTilesData, Unit>() {

    private lateinit var sectionHabitTileOne: SectionHabitTile
    private lateinit var sectionHabitTileTwo: SectionHabitTile
    private lateinit var sectionHabitTileList: List<SectionHabitTile>
    private lateinit var sectionHabitTilesData: SectionHabitTilesData
    private lateinit var sectionHabitTilesRepository: SectionHabitTilesRepository
    private lateinit var viewModel: SectionsHabitTilesViewModel

    @Before
    fun setup() {
        sectionHabitTileOne = mockk(relaxed = true)
        sectionHabitTileTwo = mockk(relaxed = true)

        sectionHabitTileList = listOf(sectionHabitTileOne, sectionHabitTileTwo)

        sectionHabitTilesData = mockk(relaxed = true) {
            every { testGroup } returns FAKE_TEST_GROUP
            every { requestId } returns FAKE_REQUEST_ID
            every { tiles } returns sectionHabitTileList
        }

        sectionHabitTilesRepository = mockk(relaxed = true) {
            coEvery { getHabitTilesFeed(any()) } returns sectionHabitTilesData
            every { canRequestPersonalizedData() } returns true
        }

        mockkStatic(Logger::class)
        every { Logger.d(any(), any()) } just Runs

        viewModel = SectionsHabitTilesViewModel(sectionHabitTilesRepository, coroutinesTestRule.dispatcher)
    }

    override fun collectUIStates(): StateFlow<SectionHabitTilesData> = viewModel.uiState

    override fun collectEvents(): SharedFlow<Unit> = MutableStateFlow(Unit)

    @Test
    fun `Given canRequestPersonalizedData is false, when fetchData is called, then uiState is not updated`() = runTest {
        every { sectionHabitTilesRepository.canRequestPersonalizedData() } returns false

        viewModelTest_runTest {
            val initialState = viewModel.uiState.value
            viewModel.fetchData()
            advanceUntilIdle()
            assertEquals(initialState, uiStates.last())
        }
    }

    @Test
    fun `Given canRequestPersonalizedData is true, when fetchData is called, then uiState is updated`() = runTest {
        viewModelTest_runTest {
            viewModel.fetchData()
            advanceUntilIdle()

            val result = uiStates.last()
            assertEquals(sectionHabitTileList, result.tiles)
            assertEquals(FAKE_REQUEST_ID, result.requestId)
            assertEquals(FAKE_TEST_GROUP, result.testGroup)
        }
    }

    @Test
    fun `getHabitTiles returns tiles after a successful fetch`() = runTest {
        viewModelTest_runTest {
            viewModel.fetchData()
            advanceUntilIdle()

            val result = viewModel.getHabitTiles()
            assertEquals(sectionHabitTileList, result)
        }
    }

    @Test
    fun `getHabitTiles returns an empty list when no fetch has occurred`() = runTest {
        viewModelTest_runTest {
            val result = viewModel.getHabitTiles()
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `getRequestId returns the correct ID after a successful fetch`() = runTest {
        viewModelTest_runTest {
            viewModel.fetchData()
            advanceUntilIdle()

            val result = viewModel.getRequestId()
            assertEquals(FAKE_REQUEST_ID, result)
        }
    }

    @Test
    fun `getRequestId returns null when no fetch has occurred`() = runTest {
        viewModelTest_runTest {
            val result = viewModel.getRequestId()
            assertNull(result)
        }
    }

    @Test
    fun `getTestGroup returns the correct group after a successful fetch`() = runTest {
        viewModelTest_runTest {
            viewModel.fetchData()
            advanceUntilIdle()

            val result = viewModel.getTestGroup()
            assertEquals(FAKE_TEST_GROUP, result)
        }
    }

    @Test
    fun `getTestGroup returns null when no fetch has occurred`() = runTest {
        viewModelTest_runTest {
            val result = viewModel.getTestGroup()
            assertNull(result)
        }
    }

    companion object {
        private const val FAKE_REQUEST_ID = "requestId"
        private const val FAKE_TEST_GROUP = "testGroup"
    }
}
