package com.wapo.flagship.wapomain.ui

import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.TopBarActionItem
import com.washingtonpost.android.paywall.models.BannerPaywallMessage

sealed class MainActivityUIEvent {

    data class DetermineTopBarState(
        val y: Float,
        val hasSectionTitle: Boolean
    ) : MainActivityUIEvent()

    data class SwitchBottomTab(
        val clickedTab: BottomTab
    ) : MainActivityUIEvent()

    object ShowSnackbar : MainActivityUIEvent()
    object OpenListenToThePost : MainActivityUIEvent()

    data class HandleTopBarAction(
        val topBarActionItem: TopBarActionItem
    ) : MainActivityUIEvent()

    object OnBackPressed : MainActivityUIEvent()
    object FallbackRetry : MainActivityUIEvent()
    data class FallbackCallSite(val url: String) : MainActivityUIEvent()

    data class FallbackArticleClicked(
        val contentUrl: String
    ) : MainActivityUIEvent()

    object LowDataModeModalSeen : MainActivityUIEvent()
    object LowDataModeAllow : MainActivityUIEvent()
    object LowDataModeDismiss : MainActivityUIEvent()
    object LowDataModeSnooze : MainActivityUIEvent()

    data class OnNotificationIconClick(
        val response: Boolean = false
    ) : MainActivityUIEvent()

    object SnackBarOpenSettings : MainActivityUIEvent()
    object SnackBarTurnOnLowDataMode : MainActivityUIEvent()
    object SnackBarDismiss : MainActivityUIEvent()

    object SnackBarResultDismissed : MainActivityUIEvent()
    data class BottomPromptActionClicked(
        val message: BannerPaywallMessage
    ) : MainActivityUIEvent()

    data class BottomPromptDismissed(
        val message: BannerPaywallMessage
    ) : MainActivityUIEvent()

}