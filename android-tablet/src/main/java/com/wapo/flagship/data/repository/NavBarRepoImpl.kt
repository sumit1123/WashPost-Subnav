package com.wapo.flagship.data.repository

import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.flagship.domain.repository.NavBarRepo
import com.wapo.flagship.domain.repository.PrefUtilsRepo
import com.wapo.flagship.navigation.ui.BottomTab
import javax.inject.Inject


class NavBarRepoImpl @Inject constructor(
    private val deviceUtilRepo: DeviceUtilRepo,
    private val prefUtilsRepo: PrefUtilsRepo
) : NavBarRepo {

    override fun getDefaultTabs(): List<BottomTab> {
        return if (deviceUtilRepo.isTablet()) {
            NavBarRepo.tabletTabs
        } else {
            NavBarRepo.mobileTabs
        }
    }

    override fun getLastVisitedBottomTab(): BottomTab {
        return prefUtilsRepo.getLastVisitedBottomTab()
            ?.let { savedRoute ->
                BottomTab.entries.find { it.route == savedRoute }
            } ?: BottomTab.Home
    }
}