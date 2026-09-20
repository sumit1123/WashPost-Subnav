package com.wapo.flagship.util.tracking

import android.view.View
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.wapo.adsinf.tracking.IAdTracker

/**
 * [com.wapo.adsinf.BannerAdView] and [com.wapo.adsinf.AdManager] class will call
 * [IAdTracker] callbacks.
 * This is the app's default implementation and all banner ads are using this class.
 */
class AdTrackerImpl : IAdTracker {
    override fun startTracking(
        uniqueId: String?,
        view: View?,
    ) {
    }

    override fun stopTracking(uniqueId: String?) {
    }

    override fun addCustomTargeting(adManagerAdRequestBuilder: AdManagerAdRequest.Builder?) {
    }
}
