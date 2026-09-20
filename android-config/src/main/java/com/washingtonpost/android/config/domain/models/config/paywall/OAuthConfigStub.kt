package com.washingtonpost.android.config.domain.models.config.paywall

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.Utils
import java.security.MessageDigest
import java.util.UUID

data class OAuthConfigStub(
    private val baseAuthorizationUrl: String,
    val signUpUrl: String,
    val freeTrialUrl: String,
    val authorizationScope: String,
    val authorizationState: String,
    val tokenUrl: String,
    val oneLinkTokenUrl: String,
    val profileUrl: String,
    val revokeUrl: String,
    val migrateUrl: String,
    val appType: String,
    val saveIdentityPreferencesUrl: String,
    val nonceUrl: String,
) {
    /**
     * Promo/Free Trial registration flow uses a different URL path than simple registration.
     */
    fun getAuthorizationUrl(
        promoId: String?,
        trialType: String?,
        isSignUp: Boolean,
        extra: PaywallServiceConfigExtra,
    ): String {
        val url = StringBuilder()
        url.append(getAuthorizationBaseUrl(isSignUp, promoId != null && trialType != null))
        url.append(getBaseQueryParameters(extra, promoId, trialType))
        if (promoId != null && trialType != null) {
            url.append(getPromoId(promoId))
            url.append(getTrialType(trialType))
        }
        Logger.d(TAG, "Authorization URL: $url")
        return url.toString()
    }

    fun getAuthorizationBaseUrl(isSignUp: Boolean, isFreeTrial: Boolean) = when {
        isSignUp -> baseAuthorizationUrl.replace("/signin", signUpUrl)
        isFreeTrial -> baseAuthorizationUrl.replace("/signin", freeTrialUrl)
        else -> baseAuthorizationUrl
    }

    private fun getBaseQueryParameters(
        extra: PaywallServiceConfigExtra,
        promoId: String?,
        trialType: String?
    ): String {
        val strBuilder = StringBuilder().apply {
            append(OS_TYPE)
            if (appType.isNotEmpty()) append(getAppType(appType))
            append(getDeviceType(Utils.isAmazonBuild()))
            append(getSuffix(extra))
            append(getDeviceID(extra.deviceId))
            append(getRequestID())
            append(getAppVersion(extra.appVersion))
            append(if (Utils.isAmazonBuild()) AMAZON_DEVICE_TYPE else PLAYSTORE_DEVICE_TYPE)
            append(getGenesisParams(extra))
            if (promoId != null && trialType != null) {
                append(getPromoId(promoId))
                append(getTrialType(trialType))
            }
        }
        return strBuilder.toString()
    }

    /**
     * "&purchased=true" : The device has an active sub and the login page should not show a subscribe/purchase option
     * "&paywalled=true": The device does not have an active sub and the user has reached the paywall limit, so the webpage should show
     * the subscribe/purchase option
     */
    private fun getSuffix(extra: PaywallServiceConfigExtra): String = when {
        extra.isPremiumUser -> PREMIUM_USER
        extra.hasBeenPaywalled -> PAYWALLED
        else -> ""
    }

    private fun getGenesisParams(extra: PaywallServiceConfigExtra): String {
        val paramsMap: MutableMap<String, String?> = HashMap()
        paramsMap["account_location"] = extra.genesisLocation
        paramsMap["subscription_location"] = extra.genesisLocation
        paramsMap["account_optimize_test"] = extra.testGroup
        paramsMap["account_device"] = if (extra.isTablet) "tablet" else "mobile"
        paramsMap["account_arcid"] = extra.arcId
        paramsMap["account_app_os"] = extra.appOS
        paramsMap["account_experience"] = extra.genesisExperience
        paramsMap["subscription_experience"] = extra.genesisExperience

        val paramsBuilder = java.lang.StringBuilder()
        for (key in paramsMap.keys) {
            if (paramsMap[key] != null) {
                paramsBuilder.append("&").append(key).append("=").append(paramsMap[key])
            }
        }
        return paramsBuilder.toString()
    }

    private val configHash = getSha256(this.toString())

    fun hasConfigurationChanged(context: Context): Boolean {
        val lastHash = getLastKnownConfigHash(context)
        return configHash != lastHash
    }

    fun acceptConfiguration(context: Context) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_HASH, configHash)
            .apply()
    }

    private fun getLastKnownConfigHash(context: Context): String? {
        val mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return mPrefs.getString(KEY_LAST_HASH, null)
    }

    private fun getSha256(value: String): String {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(value.toByteArray())
            return bytesToHex(md.digest())
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuilder()
        for (b in bytes) result.append(((b.toInt() and 0xff) + 0x100).toString(16).substring(1))
        return result.toString()
    }

    companion object {
        private val TAG = OAuthConfigStub::class.simpleName
        const val KEY_LAST_HASH = "lastHash"
        const val PREFS_NAME = "pw_prefs_name"

        //  Authorization url query parameters
        private const val OS_TYPE = "&ostype=android"
        private const val AMAZON_DEVICE_TYPE = "&devicetype=kindle"
        private const val PLAYSTORE_DEVICE_TYPE = "&devicetype=playstore"
        private const val PREMIUM_USER = "&purchased=true"
        private const val PAYWALLED = "&paywalled=true"
        private fun getAppType(type: String): String = "&apptype=$type"
        private fun getDeviceType(isAmazonBuild: Boolean): String =
            if (isAmazonBuild) AMAZON_DEVICE_TYPE else PREMIUM_USER

        private fun getPromoId(promoId: String): String = "&promo_id=$promoId"
        private fun getTrialType(trialType: String): String = "&trial_type=$trialType"
        private fun getDeviceID(deviceId: String) = "&device_id=$deviceId"
        private fun getRequestID(): String = "&request_id=" + UUID.randomUUID().toString()
        private fun getAppVersion(appVersion: String): String = "&appversion=$appVersion"
    }

    data class PaywallServiceConfigExtra(
        val isPremiumUser: Boolean,
        val hasBeenPaywalled: Boolean,
        val deviceId: String,
        val appVersion: String,
        val isTablet: Boolean,
        val appOS: String,
        val genesisLocation: String,
        val testGroup: String,
        val arcId: String,
        val genesisExperience: String,
    )
}