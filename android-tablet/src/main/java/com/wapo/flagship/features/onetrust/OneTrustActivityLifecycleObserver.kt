package com.wapo.flagship.features.onetrust

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.flagship.features.onboarding2.activity.Onboarding2Activity
import com.wapo.flagship.features.settings.SettingsActivity
import com.wapo.android.commons.util.Logger

/**
 * Class to handle OneTrust sync and banner logic.
 */
class OneTrustActivityLifecycleObserver(
    private val activity: AppCompatActivity,
) : LifecycleObserver {
    private val tag = OneTrustActivityLifecycleObserver::class.java.simpleName

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        if (activity is MainActivity) {
            OneTrustHelper.registerBroadcastReceivers()
        }
    }

    /**
     * Checks if banner needs to be shown on resume.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        showBannerIfNeeded()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        if (activity is MainActivity) {
            OneTrustHelper.unregisterBroadcastReceivers()
        }
    }

    /**
     * These 3 should be the only activities where the banner ought to appear:
     * Main - normal app launch
     * Article - deeplink
     * Settings - after logout
     */
    fun showBannerIfNeeded() {
        if (activity is MainActivity || activity is Articles2Activity || activity is SettingsActivity || activity is Onboarding2Activity) {
            Logger.d(
                tag,
                "OTDebug, showBannerIfNeeded," +
                    " activity=${activity.javaClass.simpleName}," +
                    " state=${activity.lifecycle.currentState}",
            )
            if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                OneTrustHelper.showBannerIfNeeded(activity)
            }
        }
    }
}
