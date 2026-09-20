/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship

import android.content.Context
import com.wapo.android.commons.util.LogUtil
import androidx.preference.PreferenceManager
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.push.PushConfigStub
import com.wapo.android.push.PushService
import com.wapo.flagship.config.Config
import com.wapo.flagship.config.ConfigService
import com.washingtonpost.android.R

class WearAppContext private constructor(ctx: Context) {

    private var config: Config = ConfigService.readConfig(ctx)
    private var _context: Context = ctx

    init {
        projectNumber = _context.resources.getString(R.string.project_number)
    }

    companion object {
        private const val TAG = "WearAppContext"

        private lateinit var instance: WearAppContext

        private const val PREF_IS_PREMIUM_USER = "pref_is_premium_user"
        private const val PREF_TOP_HEADLINE = "pref_top_headline"
        private const val PREF_TOP_HEADLINE_URL = "pref_top_headline_url"
        private const val PREF_IS_CONNECTED_TO_PHONE = "pref_is_connected_to_phone"
        private const val PREF_PUSH_REGISTRATION_ID = "pref_push_registration_id"
        private const val PREF_PUSH_ENABLED = "pref_push_enabled"

        private lateinit var projectNumber: String

        fun init(ctx: Context) {
            instance = WearAppContext(ctx)
        }

        fun config(): Config = getInstance().config

        private fun getInstance(): WearAppContext = instance

        var isPremiumUser: Boolean
            get() = PreferenceManager
                .getDefaultSharedPreferences(instance._context)
                .getBoolean(PREF_IS_PREMIUM_USER, false)
            set(value) {
                val editor = PreferenceManager.getDefaultSharedPreferences(instance._context).edit()
                editor.putBoolean(PREF_IS_PREMIUM_USER, value)
                editor.apply()
            }

        var topHeadline: String?
            get() = PreferenceManager
                .getDefaultSharedPreferences(instance._context)
                .getString(PREF_TOP_HEADLINE, null)
            set(topHeadline) {
                val editor = PreferenceManager.getDefaultSharedPreferences(instance._context).edit()
                editor.putString(PREF_TOP_HEADLINE, topHeadline)
                editor.apply()
            }

        var topHeadlineUrl: String?
            get() = PreferenceManager
                .getDefaultSharedPreferences(instance._context)
                .getString(PREF_TOP_HEADLINE_URL, null)
            set(topHeadlineUrl) {
                val editor = PreferenceManager.getDefaultSharedPreferences(instance._context).edit()
                editor.putString(PREF_TOP_HEADLINE_URL, topHeadlineUrl)
                editor.apply()
            }

        var isConnectedToPhone: Boolean
            get() = PreferenceManager
                .getDefaultSharedPreferences(instance._context)
                .getBoolean(PREF_IS_CONNECTED_TO_PHONE, false)
            set(value) {
                val editor = PreferenceManager.getDefaultSharedPreferences(instance._context).edit()
                editor.putBoolean(PREF_IS_CONNECTED_TO_PHONE, value)
                editor.apply()
            }

        var pushRegistrationId: String?
            get() = PreferenceManager
                .getDefaultSharedPreferences(instance._context)
                .getString(PREF_PUSH_REGISTRATION_ID, "")
            set(registrationId) {
                val editor = PreferenceManager.getDefaultSharedPreferences(instance._context).edit()
                editor.putString(PREF_PUSH_REGISTRATION_ID, registrationId)
                editor.apply()
            }

        var isPushEnabled: Boolean
            get() =
                if (!this::instance.isInitialized) true
                else PreferenceManager
                    .getDefaultSharedPreferences(instance._context)
                    .getBoolean(PREF_PUSH_ENABLED, false)
            set(pushEnabled) {
                if (!this::instance.isInitialized) return
                val editor = PreferenceManager.getDefaultSharedPreferences(instance._context).edit()
                editor.putBoolean(PREF_PUSH_ENABLED, pushEnabled)
                editor.apply()
            }

        private fun isTopicEnabled(topicKey: String): Boolean {
            return if (!this::instance.isInitialized) true
            else PreferenceManager
                .getDefaultSharedPreferences(instance._context)
                .getBoolean(topicKey, false)
        }

        fun changeTopicEnabled(topicKey: String?, enabled: Boolean) {
            val editor = PreferenceManager.getDefaultSharedPreferences(instance._context).edit()
            editor.putBoolean(topicKey, enabled)
            editor.apply()
        }

        fun checkPushStatus() {
            val availableSubscriptionTopics = getPushConfigStub().availableSubscriptionTopics

            for (topic in availableSubscriptionTopics) {
                if (isPushEnabled && (!topic.isOptional || isTopicEnabled(topic.key))
                ) {
                    LogUtil.d(TAG, "Subscribing to push topic " + topic.displayName)
                    PushService.getInstance().pushManager.enablePushTopic(topic.key, true)
                } else {
                    LogUtil.d(TAG, "Un-Subscribing to push topic " + topic.displayName)
                    PushService.getInstance().pushManager.enablePushTopic(topic.key, false)
                }
            }

            registerToDeviceMessaging()
        }

        fun handleFirstRun() {
            isPushEnabled = true
        }

        private fun getPushConfigStub(): PushConfigStub {
            val pushConfigStub = config().airshipPushConfig
            pushConfigStub!!.registrationId = pushRegistrationId
            pushConfigStub.userData = DeviceUtils.getUniqueDeviceId(instance._context)

            return pushConfigStub
        }

        private fun registerToDeviceMessaging() {
            if (pushRegistrationId.isNullOrEmpty()) {
                PushService.getInstance().registerDevice(projectNumber)
            }
        }
    }

}