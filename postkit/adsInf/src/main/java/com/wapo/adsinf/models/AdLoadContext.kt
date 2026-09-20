package com.wapo.adsinf.models

import android.util.Size
import com.washingtonpost.android.config.domain.models.config.banners.AdBidSource
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType
import com.washingtonpost.android.config.domain.models.config.banners.AdSdk
import java.util.UUID

data class AdLoadContext(
    val id: String = UUID.randomUUID().toString(),
    val adRequest: AdRequest? = null,
    val adResponse: AdResponse? = null,
    val bidResponses: List<BidResponse>? = null,
) {
    data class AdRequest(
        val sdkName: AdLoaderType,
        val section: String,
        val adUnitId: String,
        val bidSources: List<AdBidSource>? = null,
    )

    sealed class SdkResponse(val sdk: AdSdk, open val responseTime: Long)

    sealed class BidResponse(
        open val bidSource: AdBidSource,
        override val responseTime: Long,
    ) : SdkResponse(bidSource, responseTime) {
        data class Success(
            override val bidSource: AdBidSource,
            override val responseTime: Long
        ) : BidResponse(bidSource, responseTime)

        data class Failure(
            override val bidSource: AdBidSource,
            override val responseTime: Long,
            val sdkErrorMessage: String?,
            val sdkErrorCode: String?,
        ) : BidResponse(bidSource, responseTime)
    }

    sealed class AdResponse(
        open val sdkName: AdLoaderType,
        override val responseTime: Long,
    ) : SdkResponse(sdkName, responseTime) {
        data class Success(
            override val sdkName: AdLoaderType,
            override val responseTime: Long,
            val loadIndex: Int?,
            val impressionIndex: Int?,
            val winningBidSource: String?,
            val adSize: Size?,
        ) : AdResponse(sdkName, responseTime)

        data class Failure(
            override val sdkName: AdLoaderType,
            override val responseTime: Long,
            val sdkErrorMessage: String?,
            val sdkErrorCode: String?,
        ) : AdResponse(sdkName, responseTime)
    }
}
