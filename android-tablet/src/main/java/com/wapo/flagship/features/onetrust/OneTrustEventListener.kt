package com.wapo.flagship.features.onetrust

import androidx.lifecycle.MutableLiveData
import com.onetrust.otpublishers.headless.Public.OTEventListener
import com.onetrust.otpublishers.headless.Public.OTUIDisplayReason.OTUIDisplayReason
import com.wapo.android.commons.util.Logger

class OneTrustEventListener(
    private val _consentState: MutableLiveData<OneTrustConsentState>,
) : OTEventListener() {
    private val tag = OneTrustEventListener::class.java.simpleName

    override fun onShowBanner(reason: OTUIDisplayReason) {
        Logger.d(tag, "OTDebug, onShowBanner(), reason: ${reason.logReason()}")
    }

    override fun onHideBanner() {
        Logger.d(tag, "OTDebug, onHideBanner()")
    }

    override fun onBannerClickedAcceptAll() {
        Logger.d(tag, "OTDebug, onBannerClickedAcceptAll()")
        _consentState.value = OneTrustConsentState.CONSENT_GIVEN
    }

    override fun onBannerClickedRejectAll() {
        Logger.d(tag, "OTDebug, onBannerClickedRejectAll()")
        _consentState.value = OneTrustConsentState.CONSENT_GIVEN
    }

    override fun onShowPreferenceCenter(reason: OTUIDisplayReason) {
        Logger.d(tag, "OTDebug, onShowPreferenceCenter(), reason: ${reason.logReason()}")
    }

    override fun onHidePreferenceCenter() {
        Logger.d(tag, "OTDebug, onHidePreferenceCenter()")
    }

    override fun onPreferenceCenterAcceptAll() {
        Logger.d(tag, "OTDebug, onPreferenceCenterAcceptAll()")
        _consentState.value = OneTrustConsentState.CONSENT_GIVEN
    }

    override fun onPreferenceCenterRejectAll() {
        Logger.d(tag, "OTDebug, onPreferenceCenterRejectAll()")
        _consentState.value = OneTrustConsentState.CONSENT_GIVEN
    }

    override fun onPreferenceCenterConfirmChoices() {
        Logger.d(tag, "OTDebug, onPreferenceCenterConfirmChoices()")
        _consentState.value = OneTrustConsentState.CONSENT_GIVEN
    }

    override fun onShowVendorList() {
        Logger.d(tag, "OTDebug, onShowVendorList()")
    }

    override fun onHideVendorList() {
        Logger.d(tag, "OTDebug, onHideVendorList()")
    }

    override fun onVendorConfirmChoices() {
        Logger.d(tag, "OTDebug, onVendorConfirmChoices()")
        _consentState.value = OneTrustConsentState.CONSENT_GIVEN
    }

    override fun allSDKViewsDismissed(p0: String?) {
        Logger.d(tag, "OTDebug, allSDKViewsDismissed()")
    }

    override fun onVendorListVendorConsentChanged(
        p0: String?,
        p1: Int,
    ) {
        Logger.d(tag, "OTDebug, onVendorListVendorConsentChanged()")
    }

    override fun onVendorListVendorLegitimateInterestChanged(
        p0: String?,
        p1: Int,
    ) {
        Logger.d(tag, "OTDebug, onVendorListVendorLegitimateInterestChanged()")
    }

    override fun onPreferenceCenterPurposeConsentChanged(
        p0: String?,
        p1: Int,
    ) {
        Logger.d(tag, "OTDebug, onPreferenceCenterPurposeConsentChanged()")
    }

    override fun onPreferenceCenterPurposeLegitimateInterestChanged(
        p0: String?,
        p1: Int,
    ) {
        Logger.d(tag, "OTDebug, onPreferenceCenterPurposeLegitimateInterestChanged()")
    }
}
