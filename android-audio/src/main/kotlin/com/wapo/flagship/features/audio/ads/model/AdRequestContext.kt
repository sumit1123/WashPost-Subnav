package com.wapo.flagship.features.audio.ads.model

data class AdRequestContext(
    val appIdentity: AppIdentity,
    val userPrivacyConsent: UserPrivacyConsent?,
) {
    data class AppIdentity(
        val bundleId: String?,
        val storeId: String?,
        val storeUrl: String?,
        val siteUrl: String?,
    )

    data class UserPrivacyConsent(
        val gdpr: String?,
        val gdprConsent: String?,
        val usPrivacy: String?,
        val rdp: String?,
        val gpp: String? = null,
        val gppSid: String? = null,
    )
}