package com.wapo.adsinf.interfaces

import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdResult
import com.wapo.adsinf.models.AdRequest
import com.wapo.adsinf.models.AdLoadSession
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig

interface AdRequestFactory {

    suspend fun createRequest(
        adConfig: AdConfig,
        adLoaderConfig: AdLoaderConfig,
        adLoadSession: AdLoadSession,
    ): AdResult<AdRequest>

}