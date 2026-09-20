/*
 *  Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.appsFlyer

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.wapo.android.commons.util.Logger
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


/**
 * @author by Jayesh Elamgodil on 08/02/2019
 */
object AppsFlyer {

    private const val TAG = "OneLink"
    lateinit var config: Config
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val jsonAdapter = moshi.adapter(DeepLinkRoute::class.java)

    @JvmStatic
    fun init(config: Config) {
        this.config = config
    }

    @JvmStatic
    fun start(activity: Activity?) {
        activity ?: return
        if (config.enabled) {
            AppsFlyerLib.getInstance().init(config.key, WapoAppsFlyerConversionListener(), activity)
            initOneLink(config.oneLinkListener)
            startTracking(activity)
        }
    }

    @JvmStatic
    fun updateServerUninstallToken(context: Context, token: String) {
        if (config.enabled) {
            AppsFlyerLib.getInstance().updateServerUninstallToken(context, token)
        }
    }

    @JvmStatic
    fun startTracking(activity: Activity?) {
        activity ?: return
        if (config.enabled) {
            AppsFlyerLib.getInstance().start(activity)
        }
    }

    @JvmStatic
    fun stopTracking(activity: Activity?) {
        activity ?: return
        if (config.enabled) {
            AppsFlyerLib.getInstance().stop(true, activity)
        }
    }

    fun loadDeepLinkRoute(json: String): DeepLinkRoute? {
        return try {
            jsonAdapter.fromJson(json)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to parse deep link JSON: $json", e)
            null
        }
    }

    /**
     * OneLink is AppsFlyer's Unified Deep Linking feature.
         * Mobile users click on external OneLink links and are sent to specific pages in the app,
         * or first to app installation if app is not installed.
     */
    private fun initOneLink(oneLinkListener: OneLinkInterface) {
        AppsFlyerLib.getInstance().subscribeForDeepLink { deepLinkResult ->
            when (deepLinkResult.status) {
                DeepLinkResult.Status.FOUND -> {
                    val sub1 = deepLinkResult.deepLink?.getStringValue("deep_link_sub1")
                    if (!sub1.isNullOrEmpty()) {
                        Logger.d(TAG, "DeepLink authentication starting")
                        oneLinkListener.onAuthentication(sub1)
                    }
                    val sub2 = deepLinkResult.deepLink?.getStringValue("deep_link_sub2")
                    deepLinkResult.deepLink?.deepLinkValue?.let {
                        if (!sub2.isNullOrEmpty()) {
                            Logger.d(TAG, "DeepLink routing to: $sub2")
                            val route = loadDeepLinkRoute(sub2)
                            if (route == null) {
                                Logger.e(TAG, "Failed to parse deep link route: $sub2")
                                oneLinkListener.onDeepLink(it)
                                return@subscribeForDeepLink
                            }

                            val (type, subtype, id) = route
                            if (type.isNullOrEmpty() || subtype.isNullOrEmpty() || id.isNullOrEmpty()) {
                                Logger.e(
                                    TAG,
                                    "Parsed route contains null fields, falling back to deep_link_value"
                                )
                                oneLinkListener.onDeepLink(it)
                                return@subscribeForDeepLink
                            }

                            val dpValue = try {
                                Uri.parse(it)
                                    .buildUpon()
                                    .appendPath(type)
                                    .appendPath(subtype)
                                    .appendPath(id)
                                    .build()
                                    .toString()
                            } catch (e: Exception) {
                                Logger.e(
                                    TAG,
                                    "Failed to build deeplink from sub2, falling back to deep_link_value",
                                    e
                                )
                                it
                            }
                            oneLinkListener.onDeepLink(dpValue)
                        } else {
                            Logger.d(TAG, "DeepLink routing to: $it")
                            oneLinkListener.onDeepLink(it)
                        }
                    }
                }

                DeepLinkResult.Status.NOT_FOUND -> {
                    Logger.e(TAG, "DeepLink not found")
                }

                else -> {
                    val dlError = deepLinkResult.error
                    oneLinkListener.onError("OneLink error getting data: $dlError")
                }
            }
        }
    }

    data class Config(val key: String, val enabled: Boolean, val oneLinkListener: OneLinkInterface)
}


