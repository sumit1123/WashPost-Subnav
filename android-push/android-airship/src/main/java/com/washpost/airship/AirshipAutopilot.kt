package com.washpost.airship

import android.content.Context
import com.urbanairship.AirshipConfigOptions
import com.urbanairship.Autopilot
import com.urbanairship.UAirship
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.commons.util.EncryptionUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog

class AirshipAutopilot : Autopilot() {

    private enum class AirshipLogLevel(val value: Int) {
        // LogLevel is "VERBOSE"(2), "DEBUG"(3), "INFO"(4), "WARN"(5), "ERROR"(6) or "ASSERT"(7) from Logger class
        VERBOSE(2), DEBUG(3), INFO(4), WARN(5), ERROR(6), ASSERT(7)
    }

    override fun allowEarlyTakeOff(context: Context): Boolean {
        return false
    }

    override fun onAirshipReady(airship: UAirship) {
        Logger.d(TAG, "WPPush - onAirshipReady")
        AirshipProvider.privacyManager.enableDefaults()
        Logger.ds(airship)
        airship.pushManager.apply {
            userNotificationsEnabled = true
            notificationProvider = UAirshipNotificationProvider()
        }
        AirshipProvider.onAirshipReady()
        super.onAirshipReady(airship)
    }

    override fun createAirshipConfigOptions(context: Context): AirshipConfigOptions? {
        val isBetaBuild = AppContextUtils.isBetaBuild()
        val credentials = if (isBetaBuild) {
            Pair(WapoSecDataProvider.airshipKeyBeta, WapoSecDataProvider.airshipSecretBeta)
        } else {
            Pair(WapoSecDataProvider.airshipKeyProd, WapoSecDataProvider.airshipSecretProd)
        }
        val (appKey, appSecret) = credentials
        return AirshipConfigOptions.Builder()
                .setDevelopmentAppKey(WapoSecDataProvider.airshipKeyDev)
                .setDevelopmentAppSecret(WapoSecDataProvider.airshipSecretDev)
                .setProductionAppKey(appKey)
                .setProductionAppSecret(appSecret)
                .setNotificationChannel(context.getString(R.string.notification_channel_id))
                .setInProduction(!BuildConfig.DEBUG)
                .setChannelCreationDelayEnabled(true)
                .setDevelopmentLogLevel(AirshipLogLevel.DEBUG.value)
                .setProductionLogLevel(AirshipLogLevel.ERROR.value)
                .setEnabledFeatures(*AirshipPrivacyManager.defaultFeatures())
                .setIsPromptForPermissionOnUserNotificationsEnabled(false)
                .build()
    }

    private fun Logger.ds(airship: UAirship) {
        val context = UAirship.getApplicationContext()
        val loggingId = DeviceUtils.getLoggingId(context)
        val newLoggingId = DeviceUtils.getNewLoggingId(context)
        EventLog.Builder().apply {
            setMessage("onAirshipReady")
            setModule(LogModules.ALERTS)
            set("logging_id", loggingId)
            set("new_logging_id", newLoggingId)
            set("user_id", AirshipProvider.getUserId())
            set("contact_id", airship.contact.namedUserId)
            set("channel_id", airship.channel.id)
            set("subscribed_topics", airship.channel.tags)
            set("pn_status", AppContextUtils.areNotificationEnabled())
        }.run {
            val eventLog = build()
            Logger.d(TAG, eventLog.toString())
            RemoteLog.d(context, eventLog)
        }
    }

    companion object {
        private val TAG: String = AirshipAutopilot::class.java.simpleName
        private const val KEY = "8F1644079AE6DAAF60C907AD1CA9E4A5"

        private fun getValue(text: String): String {
            return EncryptionUtils.decrypt(KEY, text)
        }
    }
}