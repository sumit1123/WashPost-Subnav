// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.AppContext
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.paywall.PaywallService
import org.json.JSONObject

class IterableAppUserFields(private val iterableSdk: IterableSdk, private val context: Context) {

    private fun getBillingCountryCode(): String? {
        return PaywallService.getConnector().billingCountryCode
    }

    private fun getPushRegistrationId(): String {
        return AppContext.getRegistrationId() ?: ""
    }

    fun syncUserFields() {
        val fieldsMap = getFieldsMap()
        sync(fieldsMap)
    }

    private fun getFieldsMap(): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        return map
    }

    private fun sync(map: Map<String, String?>) {
        Logger.d(TAG, "Iterable, updateIterableField, map=$map")
        val jsonObject = JSONObject().apply {
            map.forEach { entry ->
                put(entry.key, entry.value)
            }
        }
        iterableSdk.getIterableApi().updateUser(jsonObject)
    }

    fun syncDeviceAttributes() {
        if (getPushRegistrationId().isEmpty()) {
            Logger.e(TAG, "Iterable, setDeviceAttributes, token is empty!")
            return
        }
        val storeCountryCode = getBillingCountryCode()
        val abTestAssignments = getABTestAssignments()
        Logger.d(TAG, "Iterable, setDeviceAttributes, storeCountryCode=$storeCountryCode, pushRegId=${getPushRegistrationId()}, abTestAssignments=$abTestAssignments")
        iterableSdk.getIterableApi().apply {
            setDeviceAttribute(STORE_COUNTRY_CODE_KEY, storeCountryCode)
            registerDeviceToken(getPushRegistrationId())
            setDeviceAttribute(AB_TEST_ASSIGNMENTS_KEY, abTestAssignments)
        }
    }

    /**
     * Builds the AB test assignments string from all AB test parameters.
     * Format: |test_a=group_a|test_b=group_b|
     */
    private fun getABTestAssignments(): String {
        val abVariants = StringBuilder("|")
        PrefUtils.getABParametersMap(context).forEach { (key, value) ->
            abVariants.append(key).append("=").append(value).append("|")
        }
        return abVariants.toString()
    }

    companion object {
        private const val TAG = "IterableAppUserFields"
        const val AB_TEST_ASSIGNMENTS_KEY = "abTestAssignments"
        // Keys
        const val STORE_COUNTRY_CODE_KEY = "storeCountryCode"
        const val STORE_COUNTRY_CODE_FIELD = "store_country_code"
        const val EVENT_CURRENCY_CODE_FIELD = "currency_code"
        const val PLAN_STRUCTURE_FIELD = "plan_structure"
        const val BASE_PRICE_FIELD = "base_price"
        const val CAMPAIGN_NAME_FIELD = "campaign_name"
        const val OFFER_TYPE_FIELD = "offer_type"
    }
}