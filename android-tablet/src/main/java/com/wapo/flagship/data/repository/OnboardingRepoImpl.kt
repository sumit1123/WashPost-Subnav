/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.data.repository

import android.content.Context
import androidx.core.content.edit
import com.wapo.flagship.domain.repository.OnboardingRepo
import com.wapo.flagship.domain.repository.PrefUtilsRepo
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.auth.AuthEntryPoint
import javax.inject.Inject

class OnboardingRepoImpl @Inject constructor(
    private val context: Context,
    private val prefUtilsRepo: PrefUtilsRepo,
): OnboardingRepo {

    /**
     * List of personalize screens that should be shown. These will be added as needed.
     */
    private val personalizeList = mutableListOf<Int>()

    private val screensSeen = mutableListOf<Int>()

    /**
     * Tracks whether the Continue button on each screen is activated.
     */
    private var alertsContinueActive = false
    private var contentPacksContinueActive = false

    /**
     * Get the current personalize page that the user is on
     */
    private var _currentPersonalizePage: Int = 0

    override fun shouldShowSteps() = getStepSize() > 1

    override fun shouldShowBack() = _currentPersonalizePage > 0

    override fun isFirstPage() = _currentPersonalizePage == 0

    override fun getStepSize(): Int = personalizeList.size

    override fun getCurrentStep(): Int {
        return _currentPersonalizePage + 1
    }

    override fun getCurrentPageId(): Int? {
        return if (_currentPersonalizePage < 0) {
            null
        } else {
            personalizeList[_currentPersonalizePage]
        }
    }

    override fun loadPersonalizePages(authEntryPoint: AuthEntryPoint?) {
        personalizeList.clear()
        when (authEntryPoint) {
            AuthEntryPoint.MY_POST -> loadAlertsPageIfNeeded()
            else -> {
                loadAlertsPageIfNeeded()
                loadContentPacksPageIfNeeded()
                loadAudioPageIfNeeded()
            }
        }
    }

    override fun loadAlertsPageIfNeeded() {
        if (prefUtilsRepo.shouldShowAlertsOnboarding() &&
            !personalizeList.contains(
                R.id.action_global_alertsFragment,
            )
        ) {
            personalizeList.add(R.id.action_global_alertsFragment)
        }
    }

    override fun loadContentPacksPageIfNeeded() {
        if (prefUtilsRepo.shouldShowContentPacksOnboarding() &&
            !personalizeList.contains(
                R.id.action_global_contentPacksFragment,
            )
        ) {
            personalizeList.add(R.id.action_global_contentPacksFragment)
        }
    }

    override fun loadAudioPageIfNeeded() {
        if (prefUtilsRepo.shouldShowAudioOnboarding() &&
            !personalizeList.contains(
                R.id.action_global_audioFragment,
            )
        ) {
            personalizeList.add(R.id.action_global_audioFragment)
        }
    }

    override fun isCurrentScreenAlreadySeen(): Boolean {
        return screensSeen.contains(getCurrentPageId())
    }

    override fun addSeenPage(pageId: Int) {
        screensSeen.add(pageId)
    }

    override fun nextPage() {
        val index = _currentPersonalizePage
        when {
            personalizeList.isEmpty() -> return
            index < personalizeList.size - 1 -> _currentPersonalizePage = index + 1
            else -> {}
        }
    }

    override fun previousPage() {
        val index = _currentPersonalizePage
        if (index > 0) {
            _currentPersonalizePage = index - 1
        }
    }

    override fun isAlreadyShown(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return prefs.getBoolean(FIRST_INSTALL_KEY, false)
    }

    override fun setShown() {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(FIRST_INSTALL_KEY, true) }
    }

    override fun setAlertsContinueActive(value: Boolean) {
        alertsContinueActive = value
    }

    override fun setContentPacksContinueActive(value: Boolean) {
        contentPacksContinueActive = value
    }

    override fun getAlertsContinueActive(): Boolean = alertsContinueActive

    override fun getContentPacksContinueActive(): Boolean = contentPacksContinueActive

    companion object {
        private val PREFS_FILE = "package com.wapo.flagship.features.onboarding, Unknown, version 0.0.cache.prefs"
        private val FIRST_INSTALL_KEY = "first_install_onboarding"

    }
}
