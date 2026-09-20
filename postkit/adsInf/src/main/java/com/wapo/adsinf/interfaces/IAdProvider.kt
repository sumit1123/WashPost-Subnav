package com.wapo.adsinf.interfaces

import android.content.Context
import android.os.Bundle
import com.wapo.adsinf.tracking.IAdTracker

interface IAdProvider {
    val applicationContext: Context
    val isDebugBuild: Boolean
    val ccpaAdsPrivacyString: String
    val isEURegion: Boolean
    val isTestAdsEnabled: Boolean
    val testAdsValue: String
    val appVersionName: String
    val adSubscriptionStatus: String
    val jUcid: String
    val adTracker: IAdTracker
    val hourOfDay: Int
    fun getCCPABundle(): Bundle?
    fun getSectionsAdTargetingValues(
        primarySectionId: String,
        id: String? = null,
        adCall: Any? = null,
        permutive: Any? = null,
        contentUrl: String? = null
    ): Map<String, List<String>>
}