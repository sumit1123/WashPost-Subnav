// Copyright (c) 2024 The Washington Post. All rights reserved.

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
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.preferencesapi.models.CheckPrefResponse
import com.wapo.flagship.features.preferencesapi.services.CheckPrefService
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class CheckPrefRepo
    @Inject
    constructor(
        private val checkPrefService: CheckPrefService,
        private val contentPacksRepo: ContentPacksRepo,
        private val newsprintRepo: NewsprintRepo,
        private val wallDismissalRepo: WallDismissalRepo,
        private val dispatcherProvider: DispatcherProvider,
    ) {
        suspend fun checkPref() =
            withContext(dispatcherProvider.io) {
                AuthHelper.getInstance(FlagshipApplication.getInstance().applicationContext).runWithValidToken {
                    CoroutineScope(dispatcherProvider.io).launch {
                        try {
                            val response =
                                checkPrefService.checkPref(
                                    getHeaders(),
                                    PaywallService.getInstance().loginId,
                                )
                            processCheckPref(response)
                        } catch (e: Exception) {
                            val builder: EventLog.Builder = EventLog.Builder()
                            builder
                                .setMessage("checkPref request failed")
                                .setModule(LogModules.PREFERENCES)
                                .setErrorMessage(e.message)
                                .set("cause", e.cause)
                            RemoteLog.e(FlagshipApplication.getInstance(), builder.build())
                        }
                    }
                }
            }
        private suspend fun processCheckPref(response: APIResult<CheckPrefResponse>) =
            withContext(
                dispatcherProvider.io,
            ) {
                when (response) {
                    is APIResult.Success -> {
                        val data = response.data
                        when (data?.status) {
                            "SUCCESS" -> {
                                val userPrefLastUpdated = data.lastUpdated ?: 0L
                                val context = FlagshipApplication.getInstance().applicationContext

                                // Set global modified
                                PrefUtils.setCheckPrefLmt(context, userPrefLastUpdated)
                                PreferencesSyncCoordinator.setGlobalLastModified(context, userPrefLastUpdated)
                                PreferencesSyncCoordinator.synchronize(context)
                            }
                            else -> {
                                val builder: EventLog.Builder = EventLog.Builder()
                                val message =
                                    data?.let {
                                        "checkPref returned status of ${it.status}"
                                    } ?: run {
                                        "checkPref response data was null"
                                    }
                                builder
                                    .setMessage(message)
                                    .setModule(LogModules.PREFERENCES)
                                RemoteLog.e(FlagshipApplication.getInstance(), builder.build())
                            }
                        }
                    } else -> {
                        val builder: EventLog.Builder = EventLog.Builder()
                        builder
                            .setMessage(response.getMessage())
                            .setModule(LogModules.PREFERENCES)
                        RemoteLog.e(FlagshipApplication.getInstance(), builder.build())
                    }
                }
            }

        /**
         * Required headers for the getro / setro calls.
         */
        private fun getHeaders(): HashMap<String, String> {
            val headers = getBaseHeaders()
            headers[AUTHORIZATION] = "Bearer " +
                AuthHelper
                    .getInstance(
                        FlagshipApplication.getInstance().applicationContext,
                    ).accessToken
            headers[CLIENT_ID] = PaywallService.getConnector().clientId
            headers[CLIENT_IP] = PaywallService.getConnector().ipAddress
            return headers
        }

        private fun getBaseHeaders(): HashMap<String, String> =
            hashMapOf(
                Pair(CLIENT_APP, PaywallService.getConnector().appName),
                Pair(REQUEST_ID, UUID.randomUUID().toString()),
                Pair(DEVICE_ID, PaywallService.getConnector().deviceId),
                Pair(CLIENT_USER_AGENT, PaywallService.getConnector().userAgent),
                Pair(CLIENT_APP_VERSION, PaywallService.getConnector().appVersion),
                Pair(OS_VERSION, Build.VERSION.SDK_INT.toString()),
                Pair(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL),
            )

        fun updateNewsprintPrefs() {
            CoroutineScope(Dispatchers.IO).launch {
                newsprintRepo.getUserNewsprintAttributes()
                newsprintRepo.getUserNewsprintState()
            }
        }
    }
