package com.wapo.flagship.features.sections

import android.annotation.SuppressLint
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.features.articles2.paywall.WallUiEvent
import com.wapo.flagship.features.sections.viewmodels.SectionWallHelperViewModel
import com.washingtonpost.android.paywall.util.PaywallConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SectionWallHelperViewModelTest : BaseViewModelTest<Any, WallUiEvent?>() {
    private lateinit var viewModel: SectionWallHelperViewModel

    override fun collectUIStates(): StateFlow<Any> = MutableStateFlow(Any())

    override fun collectEvents(): SharedFlow<WallUiEvent?> = viewModel.paywallEvent

    @Before
    fun setup() {
        viewModel = SectionWallHelperViewModel()
    }

    @Test
    fun sectionWallHelperViewModel_dispatchShowRegwall_Test() {
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                val wallName = "test_wall"
                val wallType =
                    PaywallConstants.WallType.REGWALL

                viewModel.dispatchShowRegwall(wallName, wallType)
                advanceUntilIdle()

                val event = events.last()
                assert(event is WallUiEvent.ShowRegwall)
                event as WallUiEvent.ShowRegwall
                assert(event.wallName == wallName)
                assert(event.wallType == wallType)
            }
        }
    }
}