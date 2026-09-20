/*
 *  Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.appsFlyer

import android.content.Context
import android.os.Process
import com.appsflyer.AFInAppEventParameterName

import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib
import java.util.concurrent.Executors

/**
 * @author by Jayesh Elamgodil on 08/02/2019
 */
object AppsFlyerMeasurement {

    const val PAYWALL_OVERLAY = "af_paywall"
    const val REGWALL_OVERLAY = "af_regwall"
    const val PAUSEWALL_OVERLAY = "af_pausewall"
    const val PAYWALL_LOGIN_SUCCESS = AFInAppEventType.LOGIN
    const val PAYWALL_REGISTER_SUCCESS = "af_register"
    const val PAYWALL_PURCHASE = AFInAppEventType.PURCHASE
    const val PAYWALL_PURCHASE_REVENUE = AFInAppEventParameterName.REVENUE
    const val PAYWALL_PURCHASE_CURRENCY = AFInAppEventParameterName.CURRENCY
    const val PAYWALL_PURCHASE_CONTENT = AFInAppEventParameterName.CONTENT
    const val PAYWALL_PURCHASE_CONTENT_ID = AFInAppEventParameterName.CONTENT_ID
    const val CONTENT_PAGE_VIEW = AFInAppEventType.CONTENT_VIEW

    private val executor = Executors.newSingleThreadExecutor { r ->
        object : Thread(r) {
            override fun run() {
                name = "AppsFlyerMeasurementThread"
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                super.run()
            }
        }
    }

    @JvmStatic
    fun trackEvent(context: Context, eventName: String, eventValue: Map<String, Any>?) {
        if (AppsFlyer.config.enabled) {
            executor.submit {
                AppsFlyerLib.getInstance().logEvent(context, eventName, eventValue)
            }
        }
    }
}