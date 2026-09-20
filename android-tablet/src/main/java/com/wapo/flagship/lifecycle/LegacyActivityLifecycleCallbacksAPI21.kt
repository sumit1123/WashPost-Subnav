package com.wapo.flagship.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.wapo.flagship.sdk.iterable.IterableSdk
import java.lang.ref.WeakReference

/**
 * Class to handle callbacks from [Application.ActivityLifecycleCallbacks] for handling application
 * level activity lifecycle callbacks that are working from [Build.VERSION_CODES.LOLLIPOP] version.
 * Note: This class can be deprecated and removed once minSdkVersion is 21 and then continue to use
 * [LegacyActivityLifecycleCallbacks] class.
 */
class LegacyActivityLifecycleCallbacksAPI21(
    override val fragmentCallbacks: AppFragmentLifecycleCallbacks,
    override val iterableSdk: IterableSdk
) : LegacyActivityLifecycleCallbacks(fragmentCallbacks, iterableSdk) {
    override fun onActivityPreCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        super.onActivityPreCreated(activity, savedInstanceState)
        // Initialize currentActivity as early as application starts (cold start or no activity was running).
        if (startedActivities == 0) {
            currentActivity = WeakReference(activity)
        }
    }
}
