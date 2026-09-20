/*
 *  Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.appsFlyer

import com.wapo.android.commons.util.Logger
import com.appsflyer.AppsFlyerConversionListener

/**
 * @author by Jayesh Elamgodil on 08/02/2019
 */
class WapoAppsFlyerConversionListener : AppsFlyerConversionListener {

    companion object {
        private val TAG = WapoAppsFlyerConversionListener::class.java.simpleName
    }

    override fun onConversionDataSuccess(map: MutableMap<String, Any>?) {
        map?.let {
            val builder = StringBuilder()
            builder.append("onConversionDataSuccess: ")
            for (attrName in map.keys) {
                builder.append("\n " + attrName + " =>> " + map[attrName])
            }
            builder.append("\n\n")
            Logger.d(TAG, builder.toString())
        }
    }

    override fun onConversionDataFail(errorMessage: String?) {
        Logger.e(TAG, "onConversionDataFail: $errorMessage")
    }

    override fun onAppOpenAttribution(map: Map<String, String>) {
        val builder = StringBuilder()
        builder.append("onAppOpenAttribution: ")
        for (attrName in map.keys) {
            builder.append("\n " + attrName + " =>> " + map[attrName])
        }
        builder.append("\n\n")
        Logger.d(TAG, builder.toString())
    }

    override fun onAttributionFailure(errorMessage: String) {
        Logger.e(TAG, "onAttributionFailure: $errorMessage")
    }
}
