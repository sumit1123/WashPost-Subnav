package com.wapo.flagship.navigation.viewmodel.navbar

import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.TopBarActionItem

sealed class NavBarEvent {

    data class NavEvent(val tab: BottomTab): NavBarEvent()
    data class TopBarAction(val item: TopBarActionItem): NavBarEvent()

    data class TabClickAgain(val route: String): NavBarEvent()

    data class ShouldPreserveState(val value: Boolean): NavBarEvent()
}