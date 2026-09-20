package com.wapo.flagship.features.snackbar

import android.annotation.SuppressLint
import com.wapo.flagship.features.lowdata.LowDataModeNotificationConfigurationImpl
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.LowDataModeNotificationState
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.features.lowdata.LowDataModeRepo
import com.wapo.flagship.snackbars.model.SnackBarType
import com.wapo.flagship.snackbars.model.SnackBarUIState
import com.wapo.flagship.snackbars.viewmodel.SnackBarViewModel
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnackBarViewModelTest : BaseViewModelTest<SnackBarUIState, Unit>() {

    private lateinit var lowDataModeRepo: LowDataModeRepo

    private lateinit var viewModel: SnackBarViewModel

    override fun collectUIStates() = viewModel.uiState

    override fun collectEvents() = MutableSharedFlow<Unit>()

    @SuppressLint("CheckResult")
    @Before
    fun setup() {
        lowDataModeRepo = mockk<LowDataModeRepo>(relaxed = true)
        coEvery { lowDataModeRepo.dismissNotification() } returns LowDataModeNotificationConfigurationImpl(
            notificationState = LowDataModeNotificationState.Dialog,
            dismissCounter = 0
        )

        viewModel = SnackBarViewModel(lowDataModeRepo)

    }

    @Test
    fun snackBarViewModel_setNoNetwork_Test() {
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.setNoNetwork(true)
                advanceUntilIdle()

                assertEquals(true, uiStates.last().noNetworkConnection)
                assert(uiStates.last().snackBarType is SnackBarType.NoNetworkConnection)
            }
        }
    }

    @Test
    fun snackBarViewModel_hideSnackBar_Test() {
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.hideSnackBar(null)
                advanceUntilIdle()

                assertEquals(null, uiStates.last().snackBarType)
            }
        }
    }

    @Test
    fun snackBarViewModel_showSnackBar_Test() {
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.showSnackBar(SnackBarType.LowDataConnection())
                advanceUntilIdle()

                assert(uiStates.last().snackBarType is SnackBarType.LowDataConnection)
            }
        }
    }

    @Test
    fun snackBarViewModel_dismissLowDataModeNotification_Test() = runTest {
        viewModelTest_runTest {

            viewModel.dismissLowDataModeNotification()
            advanceUntilIdle()
            assert(uiStates.last().lowDataModeNotificationConfig?.notificationState is LowDataModeNotificationState.Dialog)
        }
    }

    @Test
    fun snackBarViewModel_setLowDataModeNotificationState_snooze_Test() {
        runTest {
            viewModelTest_runTest {
                coEvery {  lowDataModeRepo.setLowDataModeNotificationState(any())} returns LowDataModeNotificationConfigurationImpl(
                    notificationState = LowDataModeNotificationState.Snooze(),
                    dismissCounter = 0
                )
                viewModel.snoozeLowDataModeNotification()
                advanceUntilIdle()
                assert(uiStates.last().lowDataModeNotificationConfig?.notificationState is LowDataModeNotificationState.Snooze)
            }
        }
    }

    @Test
    fun snackBarViewModel_setLowDataModeNotificationState_allow_Test() {
        runTest {
            viewModelTest_runTest {
                coEvery {  lowDataModeRepo.setLowDataModeNotificationState(any())} returns LowDataModeNotificationConfigurationImpl(
                    notificationState = LowDataModeNotificationState.Allow,
                    dismissCounter = 0
                )
                viewModel.allowLowDataModeNotification()
                advanceUntilIdle()
                assert(uiStates.last().lowDataModeNotificationConfig?.notificationState is LowDataModeNotificationState.Allow)
            }
        }
    }

    @Test
    fun snackBarViewModel_setLowDataModeNotificationState_disable_Test() {
        runTest {
            viewModelTest_runTest {
                coEvery {  lowDataModeRepo.setLowDataModeNotificationState(any())} returns LowDataModeNotificationConfigurationImpl(
                    notificationState = LowDataModeNotificationState.Disable,
                    dismissCounter = 0
                )
                viewModel.disableLowDataModeNotification()
                advanceUntilIdle()
                assert(uiStates.last().lowDataModeNotificationConfig?.notificationState is LowDataModeNotificationState.Disable)
            }
        }
    }

}