package com.wapo.flagship.features.settings

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.engagement.PageEngagementLifecycleObserver
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.onboarding2.activity.Onboarding2Activity
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.preferencesapi.repo.TopicNotificationsRepo.Companion.CONVERSATIONS_KEY
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.push.PushPreferencesHelper
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsAlertsFragment :
    BasePreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener {
    private lateinit var alertsViewModel: SettingsAlertsViewModel
    private var oneTrustBlockedDialog: AlertDialog? = null
    private val pendingNotificationOptIns = mutableSetOf<String>()

    var onSubscribed: () -> Unit = {}

    private val navBehavior: String
        get() {
            return if (activity is SettingsActivity) {
                "settings"
            } else {
                "alert"
            }
        }
    private val navArgs: SettingsAlertsFragmentArgs? by lazy {
        arguments?.let { SettingsAlertsFragmentArgs.fromBundle(it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        alertsViewModel =
            ViewModelProvider(requireActivity()).get(
                SettingsAlertsViewModel::class.java,
            )
        if (context is Onboarding2Activity) {
            alertsViewModel.entryPoint = Measurement.PROFILE_PREFERENCE_ALERTS
            alertsViewModel.showCategory = false
            alertsViewModel.isOnboarding = true
        }
    }

    override fun onResume() {
        super.onResume()
        Measurement.trackAlertSettingsPageView(navBehavior)
        processPendingNotificationOptIns()
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.pref_settings_alerts, rootKey)
        prepareTopicsPreferences(alertsViewModel.topicsList, alertsViewModel.groupList)
        if (arguments != null) {
            alertsViewModel.entryPoint = arguments?.getString(AlertsSettings.EntryPoint.TYPE) ?: alertsViewModel.entryPoint
        }
        observePageEngagement()
    }

    private fun handleEnableRequest(preference: SwitchPreferenceCompat) {
        if (!areNotificationsEnabled()) {
            showNotificationsBlockedDialog()
            pendingNotificationOptIns.add(preference.key)
            return
        }

        scrollAndEnablePreference(preference.key)
    }

    private fun processPendingNotificationOptIns(scrollToPreference: Boolean = true) {
        if (!areNotificationsEnabled()) return
        if (pendingNotificationOptIns.isEmpty()) return
        pendingNotificationOptIns.forEach { key ->
            if (scrollToPreference) {
                scrollAndEnablePreference(key)
            } else {
                enablePreference(key)
            }
        }
        pendingNotificationOptIns.clear()
    }

    private fun scrollAndEnablePreference(key: String) {
        view?.post {
            scrollToPreference(key)
            view?.postDelayed({ enablePreference(key) }, 200) //  Small delay, so user can see the change in the preference
        }
    }

    private fun enablePreference(key: String) {
        val pref = findPreference<SwitchPreferenceCompat>(key)
        if (pref != null && !pref.isChecked) {
            pref.isChecked = true
            onPreferenceChange(pref, true)
        }
    }

    private fun observePageEngagement() {
        lifecycle.addObserver(
            PageEngagementLifecycleObserver(
                pageName = Measurement.PAGE_FRONT_PREFIX + Measurement.PAGE_ALERT_SETTINGS,
                tabName = Measurement.PAGE_ALERT_SETTINGS,
                contentType = Measurement.CONTENT_TYPE_FRONT
            )
        )
    }

    override fun onPause() {
        super.onPause()
        alertsViewModel.updateProvider(requireContext())
    }

    override fun onPreferenceChange(
        preference: Preference,
        isSelected: Any,
    ): Boolean {
        val isEnabled = isSelected as Boolean
        val alertTopic = alertsViewModel.topicsList.find { it.topic.topicKey == preference.key }

        if (!areNotificationsEnabled() && isEnabled) {
            pendingNotificationOptIns.add(preference.key)
            showNotificationsBlockedDialog()
            return false
        }
        if (alertTopic == null) return true
        if (alertTopic.topic.topicKey == DAILY_READ && (!OneTrustHelper.isFunctionalityEnabled() || !OneTrustHelper.isTargetingEnabled())) {
            showOnTrustConsentReminderDialog()
            return false
        }
        AppContext.changeTopicEnabled(alertTopic.topic.topicKey, isEnabled)
        Measurement.trackAlertTopicEnroll(
            preference.title.toString(),
            alertsViewModel.entryPoint,
            isEnabled,
        )
        PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS)
        alertsViewModel.syncTopic(preference.key, isEnabled)
        onSubscribed()

        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navArgs?.enable?.let { key ->
            val pref = findPreference<SwitchPreferenceCompat>(key)
            pref?.let { handleEnableRequest(pref) }
        }
        observeOneTrustConsentState()
        removePreferenceDivider(view)
        if (settingsViewModel.isUserSignedIn() &&
            !PreferenceManager.getDefaultSharedPreferences(requireContext()).contains(CONVERSATIONS_KEY)) {
            alertsViewModel.fetchConversationNotificationPreference(requireContext())
        }
        alertsViewModel.conversationNotificationPreference.observe(viewLifecycleOwner) { isEnabled ->
            AppContext.changeTopicEnabled(CONVERSATIONS_KEY, isEnabled)
            val conversationPref = findPreference<Preference>(CONVERSATIONS_KEY)
            if (conversationPref is SwitchPreferenceCompat) {
                conversationPref.isChecked = isEnabled
            }
        }
    }

    private fun observeOneTrustConsentState() {
        alertsViewModel.oneTrustConsentState.observe(viewLifecycleOwner) { consentState ->
            val dailyReadSwitch = findPreference<SwitchPreferenceCompat>(DAILY_READ)
            if (OneTrustHelper.isTargetingEnabled() && OneTrustHelper.isFunctionalityEnabled()) {
                dailyReadSwitch?.isChecked = consentState
                Measurement.trackAlertTopicEnroll(
                    DAILY_READ,
                    AlertsSettings.EntryPoint.AUTO.trackingName,
                    consentState
                )
            }
        }
    }

    private fun showNotificationsBlockedDialog() {
        context?.let { PushPreferencesHelper.showNotificationsBlockedDialog(parentFragmentManager) }
    }

    private fun prepareTopicsPreferences(
        topics: List<AlertsSettings.AlertTopicInfo>,
        groups: List<AlertsSettings.AlertGroup>?,
    ) {
        preferenceScreen.removeAll()

        // Create a preference category for each group.
        val groupMap = mutableMapOf<String, PreferenceCategory>()
        groups?.forEach {
            PreferenceCategory(requireContext()).apply {
                title = it.label
                layoutResource = R.layout.preference_category_layout
                key = it.id
                isIconSpaceReserved = false
                preferenceScreen.addPreference(this)
                groupMap[it.id] = this
            }
        }

        // Create each preference and add to appropriate group
        topics.forEachIndexed { index, topicInfo ->
            SwitchPreferenceCompat(requireContext()).apply {
                key = topicInfo.topic.topicKey
                title = topicInfo.topic.displayName
                summary = topicInfo.topic.description
                isIconSpaceReserved = false
                isSelectable = true
                isPersistent = true
                if (index < topics.size - 1) { // to avoid applying divider to final item
                    layoutResource = R.layout.preference_layout_with_divider
                } else if (index == topics.size - 1) {
                    layoutResource = R.layout.preference_layout_without_divider
                }
                isChecked = topicInfo.isEnabled
                onPreferenceChangeListener = this@SettingsAlertsFragment
                when {
                    // If there are no groups, add preference to root screen
                    groupMap.isEmpty() -> preferenceScreen.addPreference(this)
                    // Add preference to appropriate group
                    groupMap.containsKey(topicInfo.topic.group) ->
                        groupMap[topicInfo.topic.group]?.addPreference(
                            this,
                        )
                    else -> { }
                }
            }
        }

        // Remove divider from last preference in each group
        groupMap.forEach {
            it.value.getLastPreference()?.layoutResource = R.layout.preference_layout_without_divider
        }
    }

    /**
     * Displays a reminder dialog informing the user that consent is required.
     *
     * Functionality:
     * - Creates an alert dialog with:
     *   - A title and message explaining that consent is required.
     */
    private fun showOnTrustConsentReminderDialog() {
        oneTrustBlockedDialog?.dismiss()

        val context = context ?: return
        val resources = context.resources

        oneTrustBlockedDialog = AlertDialog.Builder(context).apply {
            setTitle(resources.getString(com.washingtonpost.android.notifications.R.string.consent_blocked_title))
            setMessage(resources.getString(com.washingtonpost.android.notifications.R.string.consent_blocked_message))
            setNeutralButton(resources.getString(com.washingtonpost.android.notifications.R.string.go_settings_message)) { _, _ -> showConsentDialog() }
            setNegativeButton(resources.getString(com.washingtonpost.android.notifications.R.string.cancelLabel)) { dialog, _ -> dialog.cancel() }
        }.create().apply {
            show()
        }
    }
    /**
     * Displays the consent settings dialog where users can manage their preferences.
     *
     * Functionality:
     * - Checks if the OneTrust consent banner is currently shown.
     * - If the banner is not shown (-1), attempts to initialize OneTrust in an `AppCompatActivity`.
     * - If the banner is available, it displays the OneTrust Preference Center UI for managing consent settings.
     */
    private fun showConsentDialog() {
        val context = FlagshipApplication.getInstance()
        if (OneTrustHelper.ot.isBannerShown(context) == -1) {
            val activity = activity
            if (activity is AppCompatActivity) {
                OneTrustHelper.initSdk(activity)
            }
        } else {
            activity?.let { OneTrustHelper.ot.showPreferenceCenterUI(it) }
        }
    }

    /**
     * Gets the last preference in category.
     */
    private fun PreferenceCategory.getLastPreference(): Preference? =
        when {
            this.preferenceCount > 0 -> this.getPreference(this.preferenceCount - 1)
            else -> null
        }

    private fun areNotificationsEnabled(): Boolean {
        return PushPreferencesHelper.areNotificationsEnabled(context)
    }

    companion object {
        private const val RESOURCE_NOT_FOUND = 0
    }

    private fun removePreferenceDivider(view: View){
        view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)?.apply {
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
        }
    }
}
