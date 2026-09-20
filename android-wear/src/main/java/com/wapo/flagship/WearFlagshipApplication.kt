/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.wapo.android.commons.util.LogUtil
import androidx.wear.activity.ConfirmationActivity
import com.google.android.gms.wearable.*
import com.urbanairship.Autopilot
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.push.PushService
import com.wapo.flagship.data.MobileMessenger
import com.wapo.flagship.lifecycle.WearActivityLifecycleCallback
import com.wapo.flagship.push.WearPushListener
import com.wapo.flagship.utils.CrashWrapper
import com.washingtonpost.android.R
import com.washpost.airship.AirshipProvider
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class WearFlagshipApplication : Application() {

    // connected phone with mobile app
    var connectedMobileNode: Node? = null

    override fun onCreate() {
        super.onCreate()

        appInstance = this
        WearAppContext.init(applicationContext)
        initExteriorLibs()
        registerActivityLifecycleCallbacks(WearActivityLifecycleCallback())
    }

    private fun initExteriorLibs() {
        // initialize Crashlytics
        CrashWrapper.init()
        CrashWrapper.setUserIdentifier(DeviceUtils.getUniqueDeviceId(applicationContext))

        // only set up Airship when wear is NOT connected to phone
        if (!WearAppContext.isConnectedToPhone) {
            PushService.init(
                applicationContext,
                WearPushListener(),
                AirshipProvider,
                WearAppContext.config().airshipPushConfig
            )

            WearAppContext.handleFirstRun()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Autopilot.automaticTakeOff(this)
            }
            WearAppContext.checkPushStatus()

            val mNotificationManager =
                applicationContext.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    WearPushListener.DEFAULT_CHANNEL_ID,
                    WearPushListener.DEFAULT_CHANNEL_TITLE,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                mNotificationManager?.createNotificationChannel(channel)
            }
        }
    }

    /**
     *  Called when [com.wapo.flagship.features.homepage.activities.HomepageActivity] started
     *  and when mobile and wear are connected/disconnected.
     *
     */
    fun verifyNodeAndUpdateUi() {
        if (connectedMobileNode != null) {
            LogUtil.d(TAG, getString(R.string.installed_message))
            WearAppContext.isConnectedToPhone = true

            // disable Airship when wear is CONNECTED to phone
            if (WearAppContext.isPushEnabled) {
                WearAppContext.isPushEnabled = false
                WearAppContext.checkPushStatus()
            }

            // show confirmation that wear and mobile app are connected
            Intent(this, ConfirmationActivity::class.java).apply {
                putExtra(
                    ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.SUCCESS_ANIMATION
                )
                putExtra(
                    ConfirmationActivity.EXTRA_MESSAGE,
                    getString(R.string.connected_to_phone)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.also { startActivity(it) }

            // request user's subscription status from mobile
            val messenger = MobileMessenger(this)
            messenger.sendMessage(SUBSCRIPTION_STATUS, "")
        } else {
            LogUtil.d(TAG, getString(R.string.missing_message))
            WearAppContext.isConnectedToPhone = false
            WearAppContext.isPremiumUser = false

            // show confirmation that wear and mobile app are disconnected
            Intent(this, ConfirmationActivity::class.java).apply {
                putExtra(
                    ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.FAILURE_ANIMATION
                )
                putExtra(
                    ConfirmationActivity.EXTRA_MESSAGE,
                    getString(R.string.disconnected_to_phone_app)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.also { startActivity(it) }
        }
    }

    companion object {
        private const val TAG = "WearFlagshipApplication"
        private lateinit var appInstance: WearFlagshipApplication

        private const val PLAY_STORE_APP_URI = "market://details?id=com.washingtonpost.android"
        const val NOTIFICATION_LIST = "notification_list"
        const val NOTIFICATION_INTENT = "notification_intent"
        const val DATA_LAYER_NOTIFICATION = "DataLayerNotification"
        const val SUBSCRIPTION_STATUS = "SubscriptionStatus"

        // Create Remote Intent to open Play Store listing of app on remote device.
        private val remoteAppstoreIntent: Intent
            get() = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(PLAY_STORE_APP_URI))

        @JvmStatic
        fun getInstance(): WearFlagshipApplication {
            return appInstance
        }

        fun getAppStoreDialog(context: Context?, message: String?): AlertDialog.Builder {
            return AlertDialog.Builder(context).setMessage(message)
                .setPositiveButton("Open Google Play") { _, _ -> /*openAppInStoreOnPhone()*/ }
                .setNegativeButton("Not now") { dialog, _ -> diaLogUtil.dismiss() }
        }

//        private fun openAppInStoreOnPhone() {
//            LogUtil.d(TAG, "openAppInStoreOnPhone()")
//            val playStoreAvailabilityOnPhone =
//                PlayStoreAvailability.getPlayStoreAvailabilityOnPhone(
//                    appInstance
//                )
//            if (playStoreAvailabilityOnPhone == PlayStoreAvailability.PLAY_STORE_ON_PHONE_AVAILABLE) {
//                LogUtil.d(TAG, "\tPLAY_STORE_ON_PHONE_AVAILABLE")
//                RemoteIntent.startRemoteActivity(
//                    appInstance,
//                    remoteAppstoreIntent,
//                    null
//                )
//            } else {
//                Toast.makeText(appInstance, "Google Play unavailable", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

}