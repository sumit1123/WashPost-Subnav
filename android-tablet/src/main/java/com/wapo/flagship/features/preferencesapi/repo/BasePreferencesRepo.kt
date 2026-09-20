package com.wapo.flagship.features.preferencesapi.repo

import android.os.Build
import com.wapo.android.commons.constants.AUTHORIZATION
import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.CLIENT_APP_VERSION
import com.wapo.android.commons.constants.CLIENT_ID
import com.wapo.android.commons.constants.CLIENT_IP
import com.wapo.android.commons.constants.CLIENT_USER_AGENT
import com.wapo.android.commons.constants.DEVICE_ID
import com.wapo.android.commons.constants.DEVICE_NAME
import com.wapo.android.commons.constants.OS_VERSION
import com.wapo.android.commons.constants.REQUEST_ID
import com.wapo.flagship.FlagshipApplication
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import java.util.UUID

open class BasePreferencesRepo {
    /**
     * Required headers for the getro / setro calls.
     */
    open fun getHeaders(): HashMap<String, String> {
        val headers = getBaseHeaders()
        headers[AUTHORIZATION] =
            "Bearer " + AuthHelper.getInstance(
                FlagshipApplication.getInstance().applicationContext
            ).accessToken
        headers[CLIENT_ID] = PaywallService.getConnector().clientId
        headers[CLIENT_IP] = PaywallService.getConnector().ipAddress
        return headers
    }

    open fun getBaseHeaders(): HashMap<String, String> =
        hashMapOf(
            Pair(CLIENT_APP, PaywallService.getConnector().appName),
            Pair(REQUEST_ID, UUID.randomUUID().toString()),
            Pair(DEVICE_ID, PaywallService.getConnector().deviceId),
            Pair(CLIENT_USER_AGENT, PaywallService.getConnector().userAgent),
            Pair(CLIENT_APP_VERSION, PaywallService.getConnector().appVersion),
            Pair(OS_VERSION, Build.VERSION.SDK_INT.toString()),
            Pair(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL),
        )
}