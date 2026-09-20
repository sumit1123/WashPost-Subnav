package com.wapo.flagship.features.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.preferencesapi.repo.TopicNotificationsRepo
import com.wapo.flagship.features.preferencesapi.repo.TopicNotificationsRepo.Companion.CONVERSATIONS_KEY
import com.wapo.flagship.push.PushPreferencesHelper
import javax.inject.Inject
import androidx.core.content.edit
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SettingsAlertsViewModel
    @Inject
    constructor() : ViewModel() {
        private val alertsSettings: AlertsSettings
            get() = FlagshipApplication.getInstance().alertsSettings

        val topicsList = alertsSettings.getAlertsTopicsList()

        val groupList = alertsSettings.getAlertsGroupList()

        var showCategory: Boolean = true
        var entryPoint = ""
        var isOnboarding = false

        private val _oneTrustConsentState = LiveEvent<Boolean>()
        val oneTrustConsentState: LiveData<Boolean> = _oneTrustConsentState

        private val _conversationNotificationPreference = MutableLiveData<Boolean>()
        val conversationNotificationPreference: LiveData<Boolean> = _conversationNotificationPreference

        fun fetchConversationNotificationPreference(context: Context) {
            TopicNotificationsRepo.getInstance()
                .getConversationNotificationPreference { enabled ->
                    _conversationNotificationPreference.postValue(enabled == true)
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .edit {
                            putBoolean(CONVERSATIONS_KEY, enabled == true)
                        }
                }
        }

        fun updateProvider(context: Context) {
            PushPreferencesHelper.enablePushTopicsFromConfig(context)
            PreferencesSyncCoordinator.synchronize(context)
        }

        fun setOneTrustConsentState(allowed: Boolean) {
            _oneTrustConsentState.value = allowed
       }
        fun updateDailyRead(isSelected: Boolean) {
            AppContext.changeTopicEnabled(DAILY_READ, isSelected)
        }

        fun syncTopic(
            key: String,
            isEnabled: Boolean,
        ) {
            alertsSettings.syncTopicIfRequired(key, isEnabled)
        }
}
