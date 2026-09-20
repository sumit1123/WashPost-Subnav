package com.wapo.flagship.push

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentManager
import com.wapo.android.commons.util.Logger
import androidx.preference.PreferenceManager
import com.wapo.android.push.PushService
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.SubscriptionTopic
import com.washingtonpost.android.paywall.PaywallService

/**
 * Helper Class to handle push preferences operations.
 */
object PushPreferencesHelper {
    private const val TAG = "PushPreferencesHelper"
    private const val PREF_ONE_TIME_EP_TO_SP_DONE = "pref_one_time_ep_to_sp_done"
    private const val EDITORS_PICKS_ID = "editors_picks"
    private const val SPECIAL_REPORT_ID = "special_report"
    private const val PREF_FILE_NAME_AUTO_SUBSCRIBED_TOPICS = "PREF_FILE_NAME_AUTO_SUBSCRIBE_TOPICS"

    private fun isTopicEnabled(topicId: String): Boolean = AppContext.isTopicEnabled(topicId)

    fun onAppRun(
        context: Context,
        isFirstRun: Boolean,
    ) {
        enablePushTopicsFromConfig(context)
        // One time logic if it is not triggered before to enable "special_report" topic if
        // "editors_picks" topic is already enabled in an app update.
        if (isFirstRun) {
            setPref(context, PREF_ONE_TIME_EP_TO_SP_DONE, true)
        } else if (getPref<Boolean>(context, PREF_ONE_TIME_EP_TO_SP_DONE) == false) {
            setPref(context, PREF_ONE_TIME_EP_TO_SP_DONE, true)
            if (isTopicEnabled(EDITORS_PICKS_ID) && !isTopicEnabled(SPECIAL_REPORT_ID)) {
                AppContext.changeTopicEnabled(SPECIAL_REPORT_ID, true)
            }
        }
    }

    private fun setPref(
        context: Context,
        key: String,
        value: Any,
    ) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            else -> {
                // no-op
            }
        }
        editor.apply()
    }

    private inline fun <reified T> getPref(
        context: Context,
        key: String,
    ): T? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (T::class) {
            Boolean::class -> prefs.getBoolean(key, false) as T
            else -> null
        }
    }

    private fun isUserSignedIn(): Boolean {
        val service = PaywallService.getInstance()
        return service?.isWpUserLoggedIn == true
    }

    /**
     * Check whether alerts are enabled for each SubscriptionTopic and enable them in Airship.
     */
    fun enablePushTopicsFromConfig(context: Context) {
        val pushConfig = ConfigManager.getInstance().config.airshipPushConfig
        val availableTopics =
            if (!pushConfig.availableUserSubscriptionTopics.isNullOrEmpty() && isUserSignedIn()) {
                pushConfig.availableSubscriptionTopics + (pushConfig.availableUserSubscriptionTopics
                    ?: emptyList())
            } else {
                pushConfig.availableSubscriptionTopics
            }
        val pushManager = PushService.getInstance().pushManager
        val enabledPushTopics = pushManager.getEnabledPushTopics()

        val prefs =
            context.getSharedPreferences(
                PREF_FILE_NAME_AUTO_SUBSCRIBED_TOPICS,
                Context.MODE_PRIVATE,
            )
        val topicsToEnable = mutableListOf<String>()
        val topicsToDisable = mutableListOf<String>()
        for (topic in availableTopics) {
            if (canAutoSubscribeTopic(prefs, topic) || AppContext.isTopicEnabled(topic.key)) {
                Logger.d(PushListener.TAG, "Subscribing to push topic " + topic.displayName)
                topicsToEnable.add(topic.key)
            } else {
                Logger.d(PushListener.TAG, "Un-Subscribing to push topic " + topic.displayName)
                topicsToDisable.add(topic.key)
            }
        }
        pushManager.enablePushTopicBundle(topicsToEnable, true)
        pushManager.enablePushTopicBundle(topicsToDisable, false)

        if (enabledPushTopics != topicsToEnable.toSet()) {
            PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS)
            PreferencesSyncCoordinator.synchronize(context)
        }
    }

    private fun canAutoSubscribeTopic(
        prefs: SharedPreferences,
        topic: SubscriptionTopic?,
    ): Boolean {
        if (topic == null || topic.key == null) return false
        val topicKey = topic.key

        // Can Auto subscribe topic
        val canAutoSubscribeTopic =
            !prefs.getBoolean(topicKey, false) && !topic.isOptional
        if (canAutoSubscribeTopic) {
            with(prefs.edit()) {
                putBoolean(topicKey, true)
                apply()
            }
            // enable topic in settings
            AppContext.changeTopicEnabled(topicKey, true)
            Logger.d(PushListener.TAG, "AutoSubscribing to push topic " + topic.displayName)
        }
        return canAutoSubscribeTopic
    }

    fun areNotificationsEnabled(context: Context? = null) =
        NotificationManagerCompat
            .from(context ?: FlagshipApplication.getInstance())
            .areNotificationsEnabled()

    fun isAnyAlertSubscribed(): Boolean =
        FlagshipApplication.getInstance()
            .alertsSettings
            .getAlertsTopicsList()
            .any { it.isEnabled }

    fun showNotificationsBlockedDialog(fragmentManager: FragmentManager) {
        NotificationsBlockedDialog.show(fragmentManager)
    }
}
