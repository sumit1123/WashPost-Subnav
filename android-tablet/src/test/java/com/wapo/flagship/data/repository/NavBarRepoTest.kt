package com.wapo.flagship.data.repository

import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.flagship.domain.repository.NavBarRepo
import com.wapo.flagship.domain.repository.PrefUtilsRepo
import com.wapo.flagship.navigation.ui.BottomTab
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavBarRepoTest {

    lateinit var navBarRepo: NavBarRepo

    fun createNavBarRepo(
        deviceUtilRepo: DeviceUtilRepo = mockk<DeviceUtilRepo>(relaxed = true) {
            every { isTablet() } returns false
        },
        prefUtilsRepo: PrefUtilsRepo = mockk<PrefUtilsRepo>(relaxed = true) {
            every { getLastVisitedBottomTab() } returns BottomTab.Home.route
        }
    ): NavBarRepo {
        return NavBarRepoImpl(deviceUtilRepo, prefUtilsRepo)
    }

    @Test
    fun navBarRepo_getDefaultTabs_Tablet_Test() {
        navBarRepo = createNavBarRepo(
            deviceUtilRepo = mockk<DeviceUtilRepo>(relaxed = true) {
                every { isTablet() } returns true
            }
        )

        val result = navBarRepo.getDefaultTabs()
        assertEquals(result, NavBarRepo.tabletTabs)
    }

    @Test
    fun navBarRepo_getDefaultTabs_Mobile_Test() {
        navBarRepo = createNavBarRepo()

        val result = navBarRepo.getDefaultTabs()
        assertEquals(result, NavBarRepo.mobileTabs)
    }

    @Test
    fun navBarRepo_getLastVisitedBottomTab_home_Test() {
        navBarRepo = createNavBarRepo()

        val result = navBarRepo.getLastVisitedBottomTab()
        assertEquals(result, BottomTab.Home)
    }

    @Test
    fun navBarRepo_getLastVisitedBottomTab_Ask_Test() {
        navBarRepo = createNavBarRepo(
            prefUtilsRepo = mockk<PrefUtilsRepo>(relaxed = true) {
                every { getLastVisitedBottomTab() } returns BottomTab.Ask.route
            }
        )

        val result = navBarRepo.getLastVisitedBottomTab()
        assertEquals(result, BottomTab.Ask)
    }
}