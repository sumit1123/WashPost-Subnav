package com.wapo.flagship.features.articles2.interfaces

import com.wapo.flagship.features.articles2.models.DisclaimerInfo

interface DisclaimerInfoRepo {
    suspend fun getDisclaimerInfo(endpoint: String): DisclaimerInfo?
}