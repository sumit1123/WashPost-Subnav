package com.wapo.flagship.features.foryou

import android.content.Context
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.util.JUcidTracker
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.foryou.repo.ForYouMetaData
import com.washingtonpost.foryou.repo.ForYouMetaProvider

class ForYouMetaProviderImpl(private val context: Context) : ForYouMetaProvider {
    override fun getForYouMeta(): ForYouMetaData {
        var forYouMetaData = ForYouMetaData(
            loginId = null,
            sessionId = null,
            clientId = null,
            privacyConsentGiven = false
        )
        if (OneTrustHelper.isFunctionalityEnabled()) {
            forYouMetaData = forYouMetaData
                .copy(
                    privacyConsentGiven = true,
                    sessionId = JUcidTracker.jUcid,
                    clientId = DeviceUtils.getUniqueDeviceId(context)
                )
            if (PaywallService.initialized()) {
                forYouMetaData = forYouMetaData.copy(
                    loginId = PaywallService.getInstance().loginId
                )
            }
        }
        return forYouMetaData
    }
}
