package com.wapo.flagship.features.onboarding2.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.newsletter.repo.NewslettersRepository
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.preferencesapi.repo.ContentPacksRepo
import com.wapo.flagship.features.preferencesapi.repo.NewsprintRepo
import com.wapo.flagship.push.PushPreferencesHelper
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This view model facilitates different states in the onboarding screen(s) (probably only one)
 */
@HiltViewModel
class PostLoginViewModel
    @Inject
    constructor(
        private val dispatcherProvider: DispatcherProvider,
        private val contentPacksRepo: ContentPacksRepo,
        private val newsprintRepo: NewsprintRepo,
        private val newslettersRepository: NewslettersRepository,
        @ApplicationContext context: Context,
    ) : ViewModel() {
        /**
         * Get alert settings
         */
        private val alertsSettings: AlertsSettings
            get() = FlagshipApplication.getInstance().alertsSettings

        /**
         * Check if any topics have been subscribed to
         */
        val userContentPacksPrefsStatus
            get() = contentPacksRepo.getroContentPackStatus

        /**
         * Live Event that is posted when all checks have been cleared.
         */
        private val _allChecksCompleted: LiveEvent<Boolean> = LiveEvent()
        val allChecksCompleted: LiveData<Boolean> = _allChecksCompleted

        /**
         * Screens checked for whether to show or not
         */
        private val screensChecked = MutableList(3) { false }
        private var index = 0

        private val paywallPrefHelper = PaywallPrefHelper.getInstance(context)

        /**
         * As each screen is checked [screensChecked] will get updated.
         * When all screens have been checked, [_allChecksCompleted] will post a value of true.
         */
        fun updateScreensChecked() {
            screensChecked[index] = true
            if (index < screensChecked.lastIndex) {
                index++
            }
            if (!screensChecked.contains(false)) {
                _allChecksCompleted.postValue(true)
            }
        }

        /**
         * Check if Alerts should be shown based on conditions
         * 1. has not been shown before (value isn't false)
         * 2. no alert topic is already subscribed to.
         */
        fun updateShowAlertsPref(context: Context) {
            if (PrefUtils.shouldShowAlertsOnboarding(context)) {
                PrefUtils.setShouldShowAlertsOnboarding(
                    context,
                    !PushPreferencesHelper.areNotificationsEnabled() ||
                            !PushPreferencesHelper.isAnyAlertSubscribed()
                )
            }
            updateScreensChecked()
        }

        /**
         * Calls remote to determine if content packs have been selected.
         * Response is observed by [PostLoginActivity].
         */
        fun updateShowContentPacks() {
            viewModelScope.launch(dispatcherProvider.io) {
                contentPacksRepo.getUserContentPacks()
            }
        }

        /**
         * Check if customize audio screen should be shown based on conditions
         * 1. has not been shown before (value isn't false)
         * 2. no audio settings have been set
         */
        fun updateShowAudioPref(context: Context) {
            if (PrefUtils.shouldShowAudioOnboarding(context)) {
                PrefUtils.setShouldShowAudioOnboarding(context, true)
            }
            updateScreensChecked()
        }

        fun updateNewsprint() {
            viewModelScope.launch {
                newsprintRepo.getUserNewsprintAttributes()
                newsprintRepo.getUserNewsprintState()
            }
        }

        /**
         * Determines if 1 or more screens should be shown. This will determine if
         * Onboarding screens are to be shown or if only login toast is shown
         */
        fun shouldShowOnboarding(context: Context) =
            PrefUtils.shouldShowAlertsOnboarding(context) ||
                PrefUtils.shouldShowAudioOnboarding(context) ||
                PrefUtils.shouldShowContentPacksOnboarding(context)

        fun syncNewsletters() {
            viewModelScope.launch {
                newslettersRepository.syncNewsletters()
            }
        }
    }
