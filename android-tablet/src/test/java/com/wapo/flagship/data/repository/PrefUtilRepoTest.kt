package com.wapo.flagship.data.repository

import android.content.SharedPreferences
import com.wapo.flagship.domain.repository.PrefUtilsRepo
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PrefUtilRepoTest {

    lateinit var prefUtilsRepo: PrefUtilsRepo

    fun createPrefUtilsRepo(trueValues: Boolean = true): PrefUtilsRepo {
        val sharedPreferences: SharedPreferences = mockk<SharedPreferences>(relaxed = true) {
            every { getString(PrefUtilsRepo.PREF_LAST_VISITED_BOTTOM_TAB, any()) } returns PREF_LAST_VISITED_BOTTOM_TAB_RESULT
            every { getBoolean(PrefUtilsRepo.PREF_SHOW_ALERTS_ONBOARDING, any()) } returns trueValues
            every { getBoolean(PrefUtilsRepo.PREF_SHOW_CONTENT_PACKS_ONBOARDING, any()) } returns trueValues
            every { getBoolean(PrefUtilsRepo.PREF_SHOW_AUDIO_ONBOARDING, any()) } returns trueValues
        }


        return PrefUtilsRepoImpl(sharedPreferences)
    }

    @Test
    fun prefUtilsRepo_getLastVisitedBottomTab_Test() {
        prefUtilsRepo = createPrefUtilsRepo()

        val result = prefUtilsRepo.getLastVisitedBottomTab()
        assertEquals(result, PREF_LAST_VISITED_BOTTOM_TAB_RESULT)
    }

    @Test
    fun prefUtilsRepo_shouldShowAlertsOnboarding_true_Test() {
        prefUtilsRepo = createPrefUtilsRepo()

        val result = prefUtilsRepo.shouldShowAlertsOnboarding()
        assertEquals(result, true)
    }

    @Test
    fun prefUtilsRepo_shouldShowContentPacksOnboarding_true_Test() {
        prefUtilsRepo = createPrefUtilsRepo()

        val result = prefUtilsRepo.shouldShowContentPacksOnboarding()
        assertEquals(result, true)
    }

    @Test
    fun prefUtilsRepo_shouldShowAudioOnboarding_true_Test() {
        prefUtilsRepo = createPrefUtilsRepo()

        val result = prefUtilsRepo.shouldShowAudioOnboarding()
        assertEquals(result, true)
    }

    @Test
    fun prefUtilsRepo_shouldShowAlertsOnboarding_false_Test() {
        prefUtilsRepo = createPrefUtilsRepo(trueValues = false)

        val result = prefUtilsRepo.shouldShowAlertsOnboarding()
        assertEquals(result, false)
    }

    @Test
    fun prefUtilsRepo_shouldShowContentPacksOnboarding_false_Test() {
        prefUtilsRepo = createPrefUtilsRepo(trueValues = false)

        val result = prefUtilsRepo.shouldShowContentPacksOnboarding()
        assertEquals(result, false)
    }

    @Test
    fun prefUtilsRepo_shouldShowAudioOnboarding_false_Test() {
        prefUtilsRepo = createPrefUtilsRepo(trueValues = false)

        val result = prefUtilsRepo.shouldShowAudioOnboarding()
        assertEquals(result, false)
    }

    companion object {
        private const val PREF_LAST_VISITED_BOTTOM_TAB_RESULT = "home"
    }
}