package com.wapo.flagship.features.deeplinks

import com.iterable.iterableapi.IterableInAppFragmentHTMLNotification
import com.wapo.android.push.PushService
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.features.alerts.AlertsActivity
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.flagship.features.articles2.fragments.ArticleContentFragment
import com.wapo.flagship.features.audio.fragments.AudioPagerFragment
import com.wapo.flagship.features.mypost.fragments.MyPost2Fragment
import com.wapo.flagship.features.notification.NotificationsFragment
import com.wapo.flagship.features.onboarding.BaseOnboardingFragment
import com.wapo.flagship.features.onboarding2.activity.Onboarding2Activity
import com.wapo.flagship.features.photos.NativePhotoActivity
import com.wapo.flagship.features.print.ArchivesFragment
import com.wapo.flagship.features.print.EditionsActivity
import com.wapo.flagship.features.print.PdfActivity
import com.wapo.flagship.features.sections.SectionFrontsFragment
import com.wapo.flagship.features.settings.SettingsActivity
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.ask.ui.AskFragment
import com.washingtonpost.android.gdpr.ConsentWallFragment
import com.washingtonpost.android.paywall.auth.PaywallLoginActivity
import com.washingtonpost.android.paywall.auth.PaywallTokenActivity
import com.washingtonpost.android.paywall.bottomsheet.ui.PaywallSheet2Fragment
import com.washingtonpost.android.paywall.reminder.ReminderScreenFragment
import com.washingtonpost.android.paywall.reminder.acquisition.AcquisitionReminderFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AirshipAnalytics {
    val TAG = this.javaClass.simpleName

    enum class Screen(
        val sName: String,
    ) {
        HOME_SCREEN("home_screen"),
        ALERTS_SCREEN("alerts_screen"),
        MY_POST_SCREEN("myPost_screen"),
        PRINT_EDITION_SCREEN("printEdition_screen"),
        ARTICLES_SCREEN("articles_screen"),
        SECTION_FRONTS_SCREEN("section_fronts_screen"),
        SETTINGS_SCREEN("settings_screen"),

        ARTICLES_ACTIVITY_SCREEN("articles_activity_screen"),

        ALERTS_SETTINGS_SCREEN("alerts_settings_screen"),
        ONBOARDING_SCREEN("onboarding_screen"),
        ACQUISITION_SCREEN("acquisition_screen"),
        PAYWALL_SCREEN("paywall_screen"),
        SEARCH_SCREEN("search_screen"),
        PDF_SCREEN("pdf_screen"),
        EDITIONS_SCREEN("editions_screen"),
        PAYWALL_LOGIN_SCREEN("paywall_login_screen"),
        AUTHORIZATION_MANAGEMENT_SCREEN("authorization_management_screen"),
        NATIVE_PHOTO_SCREEN("native_photo_screen"),
        AUDIO_PLAYER_SCREEN("audio_player_screen"),
        VIDEO_SCREEN("video_screen"),
        REMINDER_SCREEN("reminder_screen"),
        CONSENT_SCREEN("consent_screen"),
        REVIEW_APP_SCREEN("review_app_screen"),
        UPDATE_APP_SCREEN("update_app_screen"),
        TOOLTIP_SCREEN("tooltip_screen"),
        PAYWALL_TOKEN_SCREEN("paywall_token_screen"),
        PAYWALL_SHEET_2_SCREEN("paywall_sheet_2_screen"),
        ITERABLE_MESSAGE_SCREEN("iterable_message_screen"),
        ASK_THE_POST_SCREEN("ask_the_post_screen"),
    }

    /**
     * Classes can request to pause/resume an automation with their [PauseReason]s.
     * Create a Reason and make use of it where the app needs.
     * Automation will be paused until [pauseReasonsSet].size > 0
     */
    enum class PauseReason {
        ARTICLE_READ_WAIT,
    }

    private val notAllowedScreenSet = linkedSetOf<Screen?>()
    private var pauseReasonsSet = linkedSetOf<PauseReason>()
    private val _activeScreensState: MutableStateFlow<List<Screen>> =
        MutableStateFlow(listOf())
    val activeScreensState: StateFlow<List<Screen>> = _activeScreensState

    fun startTracking(name: String?) {
        getScreen(name)?.let {
            if (!_activeScreensState.value.contains(it)) {
                _activeScreensState.value += it
                Logger.d(TAG, "Iterable, Analytics, startTracking, pushed $it, stack=${_activeScreensState.value}")
            }
            // Clearing set when new screen is allowed to display messages.
            // When pressing back, previous screens get re-added to the set if they are not allowed.
            if (shouldClearNotAllowedScreenSet(it)) {
                notAllowedScreenSet.clear()
            }
            // App should pause iaa automation in the screens where it is not allowed.
            // Add those screens to addScreenToNotAllowedSet to pause or resume iaa automation.
            // trackScreen should be called for all screens that are allowed and configurable from console.
            // When any screen/dialog need special logic to control pause/resume behavior, they can still call
            // addScreenToNotAllowedSet and removeScreenFromNotAllowedSet methods from outside of this class.
            if (shouldPause(it)) {
                addScreenToNotAllowedSet(it)
            }
            pauseAutomationIfNeeded()
            if (shouldTrack(it)) {
                AirshipAttributes.updateUserStatusAttribute()
                PushService.getInstance().pushManager.trackScreen(it.sName)
            }
        }
        Logger.d(
            TAG,
            "InAppMessage, startTracking, name=$name, screen=${getScreen(
                name,
            )}, notAllowedScreenSet=$notAllowedScreenSet, pauseReasonsSet=$pauseReasonsSet",
        )
    }

    fun stopTracking(name: String?) {
        getScreen(name)?.let {
            if (_activeScreensState.value.contains(it)) {
                _activeScreensState.value -= it
                Logger.d(TAG, "Iterable, Analytics, stopTracking, popped $it, stack=${_activeScreensState.value}")
            }
            removeScreenFromNotAllowedSet(it)
            pauseAutomationIfNeeded()
        }
        Logger.d(
            TAG,
            "InAppMessage, stopTracking, after, name=$name, screen=${getScreen(
                name,
            )}, notAllowedScreenSet=$notAllowedScreenSet, pauseReasonsSet=$pauseReasonsSet",
        )
    }

    private fun shouldClearNotAllowedScreenSet(screen: Screen): Boolean {
        return when (screen) {
            Screen.HOME_SCREEN, Screen.SECTION_FRONTS_SCREEN,
            Screen.ARTICLES_ACTIVITY_SCREEN, Screen.SETTINGS_SCREEN -> return true
            else -> false
        }
    }

    private fun shouldPause(screen: Screen?): Boolean =
        when (screen) {
            Screen.ALERTS_SETTINGS_SCREEN, Screen.ONBOARDING_SCREEN,
            Screen.ACQUISITION_SCREEN, Screen.PAYWALL_SCREEN, Screen.SEARCH_SCREEN,
            Screen.PDF_SCREEN, Screen.EDITIONS_SCREEN, Screen.PAYWALL_LOGIN_SCREEN,
            Screen.AUTHORIZATION_MANAGEMENT_SCREEN, Screen.NATIVE_PHOTO_SCREEN,
            Screen.AUDIO_PLAYER_SCREEN, Screen.VIDEO_SCREEN,
            Screen.REMINDER_SCREEN, Screen.CONSENT_SCREEN,
            Screen.REVIEW_APP_SCREEN, Screen.UPDATE_APP_SCREEN, Screen.TOOLTIP_SCREEN,
            Screen.PAYWALL_TOKEN_SCREEN, Screen.PAYWALL_SHEET_2_SCREEN,
            -> true
            else -> false
        }

    private fun shouldTrack(screen: Screen?): Boolean =
        when (screen) {
            Screen.HOME_SCREEN, Screen.SECTION_FRONTS_SCREEN, Screen.ALERTS_SCREEN, Screen.MY_POST_SCREEN,
            Screen.PRINT_EDITION_SCREEN, Screen.ARTICLES_SCREEN, Screen.SETTINGS_SCREEN,
            -> true
            else -> false
        }

    private fun addScreenToNotAllowedSet(screen: Screen?) {
        notAllowedScreenSet.add(screen)
    }

    private fun removeScreenFromNotAllowedSet(screen: Screen?) {
        notAllowedScreenSet.remove(screen)
    }

    /**
     * Classes can request to pause/resume the automation with [pause] and [pauseReason].
     * @param pause: true - pauses the automation; false - resumes the automation.
     */
    fun pauseMessages(
        pause: Boolean,
        pauseReason: PauseReason,
    ) {
        Logger.d(
            TAG,
            "InAppMessage, pauseMessages, pause=$pause, pauseReason=$pauseReason, pauseReasonsSet=$pauseReasonsSet",
        )
        if (pause) {
            pauseReasonsSet.add(pauseReason)
        } else {
            pauseReasonsSet.remove(pauseReason)
        }
        pauseAutomationIfNeeded()
    }

    private fun pauseAutomationIfNeeded() {
        val pauseAutomation = notAllowedScreenSet.size > 0 || pauseReasonsSet.size > 0
        PushService.getInstance().apply {
            pushManager.pauseInAppAutomation(pauseAutomation)
            iamProviders?.forEach { it.pauseInAppAutomation(pauseAutomation) }
        }
    }

    fun getScreen(name: String?): Screen? =
        when (name) {
            // Allowed for displaying messages
            MainActivity::class.java.simpleName -> Screen.HOME_SCREEN
            SectionFrontsFragment::class.java.simpleName -> Screen.SECTION_FRONTS_SCREEN
            NotificationsFragment::class.java.simpleName -> Screen.ALERTS_SCREEN
            ArchivesFragment::class.java.simpleName -> Screen.PRINT_EDITION_SCREEN
            ArticleContentFragment::class.java.simpleName -> Screen.ARTICLES_SCREEN
            SettingsActivity::class.java.simpleName -> Screen.SETTINGS_SCREEN
            // Allowed for displaying messages but not tracking
            Articles2Activity::class.java.simpleName -> Screen.ARTICLES_ACTIVITY_SCREEN
            // Not allowed for displaying messages
            AlertsActivity::class.java.simpleName -> Screen.ALERTS_SETTINGS_SCREEN
            BaseOnboardingFragment::class.java.simpleName -> Screen.ONBOARDING_SCREEN
            AcquisitionReminderFragment::class.java.simpleName -> Screen.ACQUISITION_SCREEN
            PdfActivity::class.java.simpleName -> Screen.PDF_SCREEN
            EditionsActivity::class.java.simpleName -> Screen.EDITIONS_SCREEN
            PaywallLoginActivity::class.java.simpleName -> Screen.PAYWALL_LOGIN_SCREEN
            "AuthorizationManagementActivity" -> Screen.AUTHORIZATION_MANAGEMENT_SCREEN
            NativePhotoActivity::class.java.simpleName -> Screen.NATIVE_PHOTO_SCREEN
            AudioPagerFragment::class.java.simpleName -> Screen.AUDIO_PLAYER_SCREEN
            ReminderScreenFragment::class.java.simpleName -> Screen.REMINDER_SCREEN
            ConsentWallFragment::class.java.simpleName -> Screen.CONSENT_SCREEN
            Screen.REVIEW_APP_SCREEN.name -> Screen.REVIEW_APP_SCREEN
            Screen.UPDATE_APP_SCREEN.name -> Screen.UPDATE_APP_SCREEN
            Screen.TOOLTIP_SCREEN.name -> Screen.TOOLTIP_SCREEN
            PaywallTokenActivity::class.java.simpleName -> Screen.PAYWALL_TOKEN_SCREEN
            PaywallSheet2Fragment::class.java.simpleName -> Screen.PAYWALL_SHEET_2_SCREEN
            Onboarding2Activity::class.java.simpleName -> Screen.ONBOARDING_SCREEN
            MyPost2Fragment::class.java.simpleName -> Screen.MY_POST_SCREEN
            IterableInAppFragmentHTMLNotification::class.java.simpleName -> Screen.ITERABLE_MESSAGE_SCREEN
            AskFragment::class.java.simpleName -> Screen.ASK_THE_POST_SCREEN
            else -> null
        }
}
