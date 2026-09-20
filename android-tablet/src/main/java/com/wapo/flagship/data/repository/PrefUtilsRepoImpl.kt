package com.wapo.flagship.data.repository

import android.content.SharedPreferences
import com.wapo.flagship.domain.repository.PrefUtilsRepo
import com.wapo.flagship.domain.repository.PrefUtilsRepo.Companion.PREF_LAST_VISITED_BOTTOM_TAB
import com.wapo.flagship.domain.repository.PrefUtilsRepo.Companion.PREF_SHOW_ALERTS_ONBOARDING
import com.wapo.flagship.domain.repository.PrefUtilsRepo.Companion.PREF_SHOW_AUDIO_ONBOARDING
import com.wapo.flagship.domain.repository.PrefUtilsRepo.Companion.PREF_SHOW_CONTENT_PACKS_ONBOARDING
import com.wapo.flagship.domain.repository.PrefUtilsRepo.Companion.PREF_SHOW_VIDEO_SCREEN_TOOLTIP
import androidx.core.content.edit

class PrefUtilsRepoImpl(private val sharedPreferences: SharedPreferences): PrefUtilsRepo {

    override fun getLastVisitedBottomTab(): String? {
        return sharedPreferences.getString(PREF_LAST_VISITED_BOTTOM_TAB, null)
    }

    override fun shouldShowAlertsOnboarding(): Boolean {
        return sharedPreferences.getBoolean(PREF_SHOW_ALERTS_ONBOARDING, true)
    }

    override fun shouldShowContentPacksOnboarding(): Boolean {
        return sharedPreferences.getBoolean(PREF_SHOW_CONTENT_PACKS_ONBOARDING, true)
    }

    override fun shouldShowAudioOnboarding(): Boolean {
        return sharedPreferences.getBoolean(PREF_SHOW_AUDIO_ONBOARDING, true)
    }

    override fun shouldShowVideoScreenTooltip(): Boolean {
        return sharedPreferences.getBoolean(PREF_SHOW_VIDEO_SCREEN_TOOLTIP, true)
    }

    override fun setShowVideoScreenTooltipViewed() {
        sharedPreferences.edit {
            putBoolean(PREF_SHOW_VIDEO_SCREEN_TOOLTIP, false)
        }
    }
}