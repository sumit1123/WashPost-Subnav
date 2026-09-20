package com.wapo.flagship.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.wapo.flagship.FlagshipApplication
import com.wapo.android.push.PushService
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.features.deeplinks.AirshipAnalytics
import com.wapo.flagship.features.deeplinks.AirshipAttributes
import com.wapo.flagship.features.print.PrintActivityInterface
import com.wapo.flagship.sdk.iterable.IterableSdk
import java.lang.ref.WeakReference

/**
 * Class to handle callbacks from [Application.ActivityLifecycleCallbacks] for handling application
 * level activity lifecycle callbacks that are working from minSdkVersion version.
 * Also refer subclass [LegacyActivityLifecycleCallbacksAPI21] of this class.
 */
open class LegacyActivityLifecycleCallbacks(
    open val fragmentCallbacks: AppFragmentLifecycleCallbacks,
    open val iterableSdk: IterableSdk
) : Application.ActivityLifecycleCallbacks {
    open var startedActivities = 0
    open var currentActivity: WeakReference<Activity>? = null

    override fun onActivityPostPaused(activity: Activity) {
        activity.apply {
            AirshipAnalytics.stopTracking(this::class.java.simpleName)
        }
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityPreResumed(activity: Activity) {
        activity.apply {
            AirshipAnalytics.startTracking(this::class.java.simpleName)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
        FlagshipApplication.getInstance().isActivityPrintRelated =
            // TODO add Articles2Activity.isPrintOriginated check
            activity is PrintActivityInterface ||
            activity is MainActivity &&
            activity.isCurrentTabPrintEdition()
        FlagshipApplication.getInstance().videoManager.onActivityResume()
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities == 0) {
            // Enabling Airship registration in onResume of the app to register only active users.
            PushService.getInstance().pushManager.enableRegistration()
            FlagshipApplication.getInstance().alertsSettings.migrateSegments()
            iterableSdk.syncIamMessages()
        }
        startedActivities++
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--
        if (startedActivities == 0) {
            AirshipAttributes.apply {
                // Update user_status attribute  and also reset initial wait value for the attribute values
                // to be stable in an app resume for IAA.
                updateUserStatusAttribute()
                updateFeaturesAttribute()
                updateIdentityUUIDAttribute()
                resetInitialWait()

            }
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        activity.apply {
            (this as? FragmentActivity)?.supportFragmentManager?.registerFragmentLifecycleCallbacks(
                fragmentCallbacks,
                true,
            )
        }
    }

    fun getCurrentActivity(): Activity? = currentActivity?.get()
}
