package com.wapo.flagship.features.navigation

import android.annotation.SuppressLint
import com.wapo.flagship.domain.repository.NavBarRepo
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.TopBarActionItem
import com.wapo.flagship.navigation.viewmodel.navbar.NavBarEvent
import com.wapo.flagship.navigation.viewmodel.navbar.NavBarUiState
import com.wapo.flagship.navigation.viewmodel.navbar.NavBarViewModel
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavBarViewModelTest : BaseViewModelTest<NavBarUiState, NavBarEvent?>() {

    private lateinit var navBarRepo: NavBarRepo
    private lateinit var viewModel: NavBarViewModel

    private val defaultTabs = listOf(BottomTab.Home, BottomTab.Ask)

    @SuppressLint("CheckResult")
    @Before
    fun setup() {
        navBarRepo = mockk<NavBarRepo>(relaxed = true)
        every { navBarRepo.getDefaultTabs() } returns defaultTabs
        every { navBarRepo.getLastVisitedBottomTab() } returns BottomTab.Home

        viewModel = NavBarViewModel(navBarRepo)
    }

    override fun collectUIStates(): StateFlow<NavBarUiState> {
        return viewModel.uiState
    }

    override fun collectEvents(): SharedFlow<NavBarEvent?> {
        return viewModel.navBarEvent
    }

    @Test
    fun navBarViewModel_navigateToTab_Initial_Home_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                advanceUntilIdle()

                assertEquals(BottomTab.Home, uiStates.last().currentTab)
            }
        }

    @Test
    fun navBarViewModel_navigateToTab_Ask_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {

                viewModel.navigateToTab(BottomTab.Ask)
                advanceUntilIdle()

                assertEquals(BottomTab.Ask, uiStates.last().currentTab)
                assert(events.contains(NavBarEvent.NavEvent(BottomTab.Ask)))
                assert(events.contains(NavBarEvent.ShouldPreserveState(true)))
            }
        }

    @Test
    fun navBarViewModel_navigateToTab_Ask_Again_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {

                viewModel.navigateToTab(BottomTab.Ask)
                advanceUntilIdle()

                viewModel.navigateToTab(BottomTab.Ask)
                advanceUntilIdle()

                assertEquals(BottomTab.Ask, uiStates.last().currentTab)
                assert(events.contains(NavBarEvent.NavEvent(BottomTab.Ask)))
                assert(events.contains(NavBarEvent.ShouldPreserveState(true)))
                assert(events.contains(NavBarEvent.TabClickAgain(BottomTab.Ask.route)))
            }
        }

    @Test
    fun navBarViewModel_navigateToTab_HOME_first_call_with_same_current_tab_and_target_tab_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                advanceUntilIdle()
                viewModel.navigateToTab(BottomTab.Home, firstCall = true)
                advanceUntilIdle()

                assertEquals(BottomTab.Home, uiStates.last().currentTab)
                assert(events.contains(NavBarEvent.NavEvent(BottomTab.Home)))
                assert(events.contains(NavBarEvent.ShouldPreserveState(true)))
            }
        }

    @Test
    fun navBarViewModel_handleTopBarAction_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.handleTopBarAction(TopBarActionItem.Settings)
                advanceUntilIdle()

                assertEquals(TopBarActionItem.Settings, uiStates.last().topBarAction)
                assert(events.contains(NavBarEvent.TopBarAction(TopBarActionItem.Settings)))
            }
        }

    @Test
    fun navBarViewModel_showBackButton_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val show = true
            val notShow = false

            viewModelTest_runTest {
                viewModel.showBackButton(show)
                advanceUntilIdle()

                assertEquals(show, uiStates.last().showBackButton)

                viewModel.showBackButton(notShow)
                advanceUntilIdle()

                assertEquals(notShow, uiStates.last().showBackButton)
            }
        }

    @Test
    fun navBarViewModel_setNavBarVisibility_visible_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val visible = true
            val notVisible = true
            viewModelTest_runTest {
                viewModel.setNavBarVisibility(visible)
                advanceUntilIdle()

                assertEquals(visible, uiStates.last().isVisible)

                viewModel.setNavBarVisibility(notVisible)
                advanceUntilIdle()

                assertEquals(notVisible, uiStates.last().isVisible)
            }
        }

    @Test
    fun navBarViewModel_setTabs_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val testTabs = listOf(BottomTab.Ask, BottomTab.Home)
            viewModelTest_runTest {
                viewModel.setTabs(testTabs)
                advanceUntilIdle()

                assert(uiStates.last().tabs == testTabs)
            }
        }

    @Test
    fun navBarViewModel_setBadge_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val show = true
            val notShow = false

            viewModelTest_runTest {
                viewModel.setBadge(show)
                advanceUntilIdle()

                assertEquals(show, uiStates.last().showBadge)

                viewModel.setBadge(notShow)
                advanceUntilIdle()

                assertEquals(notShow, uiStates.last().showBadge)
            }
        }

    @Test
    fun navBarViewModel_setLowDataModeEnable_enable_lowDataMode_fromSettings_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val isLowDataModeEnable = true
            val isFromSettings = true

            viewModelTest_runTest {
                viewModel.setLowDataModeEnable(isLowDataModeEnable, isFromSettings)
                advanceUntilIdle()

                assertEquals(BottomTab.Home, uiStates.last().currentTab)
            }
        }

    @Test
    fun navBarViewModel_setLowDataModeEnable_disable_lowDataMode_fromSettings_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val isLowDataModeEnable = false
            val isFromSettings = true

            viewModelTest_runTest {
                viewModel.setLowDataModeEnable(isLowDataModeEnable, isFromSettings)
                advanceUntilIdle()

                assertEquals(!isLowDataModeEnable, uiStates.last().isVisible)
            }
        }

    @Test
    fun navBarViewModel_setLowDataModeEnable_disable_lowDataMode_not_fromSettings_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val isLowDataModeEnable = false
            val isFromSettings = false

            viewModelTest_runTest {
                viewModel.setLowDataModeEnable(isLowDataModeEnable, isFromSettings)
                advanceUntilIdle()

                assertEquals(!isLowDataModeEnable, uiStates.last().isVisible)
                assert(uiStates.last().tabs == navBarRepo.getDefaultTabs())
            }
        }

    @Test
    fun navBarViewModel_setLowDataModeEnable_lowDataMode_not_fromSettings_currentTab_Home_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val isLowDataModeEnable = true
            val isFromSettings = false

            viewModelTest_runTest {
                viewModel.navigateToTab(BottomTab.Home)
                viewModel.setLowDataModeEnable(isLowDataModeEnable, isFromSettings)
                advanceUntilIdle()

                assertEquals(false, uiStates.last().isVisible)
            }
        }

    @Test
    fun navBarViewModel_setLowDataModeEnable_lowDataMode_not_fromSettings_currentTab_not_Home_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            val isLowDataModeEnable = true
            val isFromSettings = false

            viewModelTest_runTest {
                viewModel.navigateToTab(BottomTab.Ask)
                viewModel.setLowDataModeEnable(isLowDataModeEnable, isFromSettings)
                advanceUntilIdle()

                assertEquals(true, uiStates.last().isVisible)
                assert(uiStates.last().tabs == listOf(BottomTab.Home))
            }
        }

    @Test
    fun navBarViewModel_getCurrentTab_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                advanceUntilIdle()

                assertEquals(BottomTab.Home, viewModel.getCurrentTab())

                viewModel.navigateToTab(BottomTab.Ask)
                advanceUntilIdle()

                assertEquals(BottomTab.Ask, viewModel.getCurrentTab())
            }
        }

    @Test
    fun navBarViewModel_isCurrentTab_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                advanceUntilIdle()

                assertEquals(true, viewModel.isCurrentTab(BottomTab.Home))
                assertEquals(false, viewModel.isCurrentTab(BottomTab.Ask))
            }
        }

    @Test
    fun navBarViewModel_isRoute_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                advanceUntilIdle()

                assertEquals(true, viewModel.isRoute(BottomTab.Home.route))
                assertEquals(false, viewModel.isRoute(BottomTab.Ask.route))
            }
        }

    @Test
    fun navBarViewModel_getCurrentTabTrackingName_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.navigateToTab(BottomTab.Ask)
                advanceUntilIdle()

                assertEquals(BottomTab.Ask.trackingName, viewModel.getCurrentTabTrackingName())
            }
        }
}