/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.foryou

import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.domain.UtilsRepo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.sections.utils.JTidTracker
import com.wapo.flagship.util.JUcidTracker
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.userhistory.repo.AccessToken
import com.washingtonpost.userhistory.repo.UserHistoryMetaData
import com.washingtonpost.userhistory.repo.UserHistoryMetaProvider

class UserHistoryMetaProviderImpl(
    private val deviceUtilRepo: DeviceUtilRepo,
    private val utilsRepo: UtilsRepo
) : UserHistoryMetaProvider {

    // Tracks the last observed consent state so we only emit the "consent not given"
    // breadcrumb when the state actually changes, instead of on every call.
    private var lastConsentState: Triple<Boolean, Boolean, Boolean>? = null

    override fun getUserHistoryMeta(): UserHistoryMetaData {
        var userHistoryMetaData = UserHistoryMetaData(
            loginId = null,
            jucId = null,
            jtId = null,
            clientId = null,
            deviceId = null,
            appVersion = null,
            platform =  null,
            privacyConsentGiven = false
        )
        val functionalEnabled = OneTrustHelper.isFunctionalityEnabled()
        val targetingEnabled = OneTrustHelper.isTargetingEnabled()
        val performanceEnabled = OneTrustHelper.isPerformanceEnabled()
        val consentGiven = functionalEnabled && targetingEnabled && performanceEnabled

        if (consentGiven) {
            userHistoryMetaData = userHistoryMetaData.copy(
                privacyConsentGiven = true,
                jucId = JUcidTracker.jUcid,
                jtId = JTidTracker.currentJTid,
                clientId = deviceUtilRepo.getUniqueDeviceId(),
                deviceId = deviceUtilRepo.getUniqueDeviceId(),
                appVersion = utilsRepo.getAppVersionName(),
                platform = if (utilsRepo.isAmazonBuild()) "Amazon" else "Android"
            )
            if (PaywallService.initialized()) {
                userHistoryMetaData = userHistoryMetaData.copy(
                    loginId = PaywallService.getInstance().loginId
                )
            }
            if (userHistoryMetaData.jucId == null) {
                RemoteLog.e(
                    AppContextUtils.appContext,
                    EventLog.Builder()
                        .setMessage("Breadcrumb - j_ucid is null at UserHistoryMetaProvider despite consent given")
                        .setModule(LogModules.FOR_YOU)
                        .build()
                )
            }
        }

        val currentConsentState = Triple(functionalEnabled, targetingEnabled, performanceEnabled)
        if (!consentGiven && currentConsentState != lastConsentState) {
            RemoteLog.e(
                AppContextUtils.appContext,
                EventLog.Builder()
                    .setMessage(
                        "Breadcrumb - RTE events skipped: privacy consent not given " +
                            "(functional=$functionalEnabled, targeting=$targetingEnabled, performance=$performanceEnabled)"
                    )
                    .setModule(LogModules.FOR_YOU)
                    .build()
            )
        }
        lastConsentState = currentConsentState

        return userHistoryMetaData
    }

    override fun getAccessToken(): AccessToken {
        var accessToken = AccessToken(
            accessToken = AuthHelper.getInstance(AppContextUtils.appContext).accessToken
        )
        return accessToken
    }
}
