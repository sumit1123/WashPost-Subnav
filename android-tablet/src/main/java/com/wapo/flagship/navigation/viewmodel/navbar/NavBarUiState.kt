package com.wapo.flagship.navigation.viewmodel.navbar

import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.TopBarActionItem

data class NavBarUiState(
    val currentTab: BottomTab = BottomTab.Home,
    val tabs: List<BottomTab> = listOf(),
    val isVisible: Boolean = true,
    val showBackButton: Boolean = false,
    val showBadge: Boolean = false,
    val topBarAction: TopBarActionItem? = null
)
