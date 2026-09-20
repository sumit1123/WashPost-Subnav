package com.wapo.flagship.domain.repository

import com.wapo.flagship.navigation.ui.BottomTab

interface NavBarRepo {

    fun getDefaultTabs(): List<BottomTab>

    fun getLastVisitedBottomTab(): BottomTab

    companion object {
        val tabletTabs = listOf(
            BottomTab.Home,
            BottomTab.Ask,
            BottomTab.Listen,
            BottomTab.Watch,
            BottomTab.Games,
            BottomTab.Print,
        )

        val mobileTabs = listOf(
            BottomTab.Home,
            BottomTab.Ask,
            BottomTab.Listen,
            BottomTab.Watch,
            BottomTab.Games,
        )
    }
}