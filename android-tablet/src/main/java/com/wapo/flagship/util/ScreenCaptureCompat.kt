package com.wapo.flagship.util

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import com.wapo.android.commons.util.Logger

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
object ScreenCaptureCompat {
    var screenCaptureCallback: Activity.ScreenCaptureCallback? = null
    fun register(
        activity: Activity, callback: Activity.ScreenCaptureCallback
    ) {
        try {
            if (screenCaptureCallback == null) {
                activity.registerScreenCaptureCallback(
                    activity.mainExecutor, callback
                )
                this.screenCaptureCallback = callback
            } else {
                activity.unregisterScreenCaptureCallback(screenCaptureCallback!!)
                activity.registerScreenCaptureCallback(
                    activity.mainExecutor, callback
                )
                this.screenCaptureCallback = callback
            }
        } catch (e: Exception) {
            Logger.e(
                "ScreenCaptureCompat",
                "Failed to register screen capture callback - ${e.message}"
            )
            this.screenCaptureCallback = null
        }
    }

    fun unregister(activity: Activity) {
        try {
            if (screenCaptureCallback != null) {
                activity.unregisterScreenCaptureCallback(screenCaptureCallback!!)
                screenCaptureCallback = null
            }
        } catch (e: Exception) {
            Logger.e(
                "ScreenCaptureCompat",
                "Failed to unregister screen capture callback - ${e.message}"
            )
            this.screenCaptureCallback = null
        }
    }
}