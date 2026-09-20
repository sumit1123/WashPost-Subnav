package com.wapo.flagship.wapomain.ui

import com.wapo.flagship.features.backendhealth.models.FailoverState
import com.wapo.flagship.features.lowdata.LowDataModeNotificationConfigurationImpl
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner
import com.wapo.flagship.features.search2.events.ConversationItem
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.TopBarState
import com.wapo.flagship.snackbars.model.SnackBarType
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

data class MainActivityUIState (
    val appBarState: TopBarState?,
    val destination: BottomTab,
    val snackbar: SnackBarType?,
    val lowDataModeNotificationConfig: LowDataModeNotificationConfigurationImpl?,
    val lowDataBannerState: LowDataBanner?,
    val isLowDataModeEnable: Boolean,
    val isNightModeEnable: Boolean,
    val isAdFree: Boolean,
    val appResumeCount: Long,
    val bottomTabState: MainActivityBottomTabState,
    val topAppBarState: MainActivityTopAppBarState,
    val bottomBarsState: MainActivityBottomBarsState,
    val fallbackState: MainActivityFallbackState,
)

data class MainActivityBottomTabState(
    val bottomTabContainerId: Int,
    val isVisibleBottomNav: Boolean,
    val bottomNavTabs: List<BottomTab>,
    val currentTab: BottomTab
)

data class MainActivityTopAppBarState(
    val toolBarState: ToolBarUIState,
    val showBackButton: Boolean?,
    val showAlertBadge: Boolean?,
    val sectionTitle: String?,
    val conversationHistory: List<ConversationItem>,
)

data class MainActivityBottomBarsState(
    val shouldShowPersistentPlayer: Boolean?,
    val bottomSheetPrompt: BannerPaywallMessage? = null
)

data class MainActivityFallbackState(
    val failoverState: FailoverState
)

data class ToolBarUIState(
    val isPrivateMode: Boolean,
    val inResponseScreen: Boolean,
    val isUserLoggedIn: Boolean,
    val showTooltip: Boolean,
    val canShareChat: Boolean,
    val showScreenShotToolTip: Boolean,
)