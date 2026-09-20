package com.wapo.flagship.navigation.viewmodel.navbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.content.notifications.NotificationData
import com.wapo.flagship.domain.repository.NavBarRepo
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.TopBarActionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rx.Observable
import rx.Subscriber
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Inject

/**
 * Shared ViewModel for Top and Bottom nav bars.
 */
@HiltViewModel
class NavBarViewModel @Inject constructor(
    private val navBarRepo: NavBarRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        NavBarUiState(
            currentTab = navBarRepo.getLastVisitedBottomTab(),
            tabs = navBarRepo.getDefaultTabs()
        )
    )
    val uiState: StateFlow<NavBarUiState> = _uiState.asStateFlow()

    private val _navBarEvent: MutableSharedFlow<NavBarEvent?> = MutableSharedFlow(replay = 0)
    val navBarEvent: SharedFlow<NavBarEvent?> = _navBarEvent

    private var badgeSubscription: Subscription? = null

    fun navigateToTab(
        bottomTab: BottomTab,
        shouldPreserveState: Boolean = true,
        firstCall: Boolean = false
    ) {
        if (_uiState.value.currentTab != bottomTab || firstCall) {
            _uiState.update {
                it.copy(
                    currentTab = bottomTab
                )
            }

            viewModelScope.launch {
                _navBarEvent.emit(NavBarEvent.NavEvent(bottomTab))

                // Post ShouldPreserveState event
                _navBarEvent.emit(NavBarEvent.ShouldPreserveState(shouldPreserveState))
            }
        } else {
            viewModelScope.launch {
                _navBarEvent.emit(NavBarEvent.TabClickAgain(bottomTab.route))
            }
        }
    }

    fun handleTopBarAction(action: TopBarActionItem) {
        _uiState.update { it.copy(topBarAction = action) }
        viewModelScope.launch {
            _navBarEvent.emit(NavBarEvent.TopBarAction(action))
        }
    }

    fun showBackButton(show: Boolean) {
        _uiState.update { it.copy(showBackButton = show) }
    }

    fun setNavBarVisibility(visible: Boolean) {
        _uiState.update { it.copy(isVisible = visible) }
    }

    fun setTabs(tabs: List<BottomTab>) {
        _uiState.update { it.copy(tabs = tabs) }
    }

    fun getCurrentTab(): BottomTab = _uiState.value.currentTab

    fun isCurrentTab(bottomTab: BottomTab): Boolean = _uiState.value.currentTab == bottomTab

    fun isRoute(route: String): Boolean = _uiState.value.currentTab.route == route

    fun getCurrentTabTrackingName(): String = _uiState.value.currentTab.trackingName

    fun setBadge(showBadge: Boolean) {
        _uiState.update { it.copy(showBadge = showBadge) }
    }

    fun subscribeAlertsBadge() {
        badgeSubscription =
            Observable
                .just(FlagshipApplication.getInstance().contentManager)
                .flatMap { cm -> cm.recentNotifications }
                .map { nd -> nd.filter { !it.isRead } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    object : Subscriber<List<NotificationData>>() {
                        override fun onCompleted() {}

                        override fun onError(e: Throwable?) {
                            Logger.d("Notifications", "failed", e)
                        }

                        override fun onNext(notifications: List<NotificationData>?) {
                            setBadge(notifications?.isNotEmpty() == true || AppContext.getAlertsLaunchCount() == 0)
                        }
                    },
                )
    }

    fun unsubscribeAlertsBadge() {
        badgeSubscription?.unsubscribe()
        badgeSubscription = null
    }

    fun setLowDataModeEnable(
        isLowDataModeEnable: Boolean,
        isFromSettings: Boolean,
    ) {
        if (isFromSettings) {
            if (isLowDataModeEnable) {
                navigateToTab(BottomTab.Home)
            }
            setNavBarVisibility(!isLowDataModeEnable)
        } else {
            if (isLowDataModeEnable) {
                if (uiState.value.currentTab == BottomTab.Home) {
                    setNavBarVisibility(false)
                } else {
                    setNavBarVisibility(true)
                    setTabs(listOf(BottomTab.Home))
                }
            } else {
                setNavBarVisibility(true)
                setTabs(navBarRepo.getDefaultTabs())
            }
        }
    }
}

