package com.wapo.flagship.features.topicfollow.viewmodels

import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.FlagshipApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TopicFollowCollaborationViewModel
    @Inject
    constructor() : ViewModel() {
        private val _reloadTopicState = MutableLiveData<String>()
        val reloadTopicState: LiveData<String> = _reloadTopicState

        private val _isLowDataModeEnable = LiveEvent<Boolean>()
        val isLowDataModeEnable: LiveData<Boolean> = _isLowDataModeEnable

        fun updateTopicState(id: String) {
            _reloadTopicState.value = id
        }

        fun setIsLowDataMode(enable: Boolean) {
            _isLowDataModeEnable.value = enable
        }

        private var _pendingNotificationToggleEvent = LiveEvent<Boolean>()
        val pendingNotificationToggleEvent: LiveEvent<Boolean>
            get() = _pendingNotificationToggleEvent

        fun dispatchPendingNotificationToggleEvent() {
            val areNotificationsEnabled =
                NotificationManagerCompat
                    .from(
                        FlagshipApplication.getInstance().applicationContext,
                    ).areNotificationsEnabled()
            if (areNotificationsEnabled != _pendingNotificationToggleEvent.value) {
                _pendingNotificationToggleEvent.postValue(areNotificationsEnabled)
            }
        }
    }
