package com.wapo.flagship.features.articles2.repo

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.articles2.interfaces.DisclaimerInfoRepo
import com.wapo.flagship.features.articles2.models.DisclaimerInfo
import com.wapo.flagship.features.articles2.services.DisclaimerInfoService
import com.wapo.flagship.network.retrofit.network.APIResult
import javax.inject.Inject

class DisclaimerInfoRepoImpl @Inject constructor(
    private val disclaimerInfoService: DisclaimerInfoService
) : DisclaimerInfoRepo {

    override suspend fun getDisclaimerInfo(endpoint: String): DisclaimerInfo? {
        return when(val service = disclaimerInfoService.getDisclaimerInfo(endpoint)) {
            is APIResult.Failure -> {
                EventLog.Builder().apply {
                    setMessage("Failed to load disclaimer info")
                    setModule(LogModules.DISCLAIMER_INFO)
                    setErrorMessage(service.getMessage())
                }.run {
                    RemoteLog.e(AppContextUtils.appContext, build())
                }
                null
            }
            is APIResult.NetworkError -> {
                Logger.d("DisclaimerInfoRepoImpl", "Network error: ${service.error}")
                null
            }
            is APIResult.Success -> {
                service.data
            }
        }
    }
}