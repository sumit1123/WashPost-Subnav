package com.wapo.flagship.lifecycle

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.wapo.flagship.features.deeplinks.AirshipAnalytics

class AppFragmentLifecycleCallbacks : FragmentManager.FragmentLifecycleCallbacks() {
    override fun onFragmentResumed(
        fm: FragmentManager,
        f: Fragment,
    ) {
        AirshipAnalytics.startTracking(f::class.java.simpleName)
    }

    override fun onFragmentPaused(
        fm: FragmentManager,
        f: Fragment,
    ) {
        AirshipAnalytics.stopTracking(f::class.java.simpleName)
    }
}
