package com.wapo.adsinf.sdk.google

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdError
import com.wapo.adsinf.models.AdError.ErrorType

object GoogleAdsUtils {

    fun convertToGmsAdSize(adSize: AdDimension): AdSize? {
        return when (adSize) {
            AdDimension.Medium -> AdSize.MEDIUM_RECTANGLE
            AdDimension.Fluid -> AdSize.FLUID
            AdDimension.Banner728x90 -> AdSize.LEADERBOARD
            else -> AdSize(adSize.w, adSize.h)
        }
    }

    fun convertToGmsAdSizesList(adDimensions: List<AdDimension>): List<AdSize> {
        return adDimensions
            .mapNotNull { convertToGmsAdSize(it) }
            .ifEmpty { listOf(AdSize.MEDIUM_RECTANGLE) }
    }

    fun LoadAdError.toAdError(): AdError {
        return AdError(
            type = getErrorType(this),
            message = this.message,
        )
    }

    fun getErrorType(error: LoadAdError): ErrorType {
        return when (error.code) {
            AdRequest.ERROR_CODE_NO_FILL -> ErrorType.NO_FILL
            AdRequest.ERROR_CODE_NETWORK_ERROR -> ErrorType.NETWORK_ERROR
            AdRequest.ERROR_CODE_INVALID_REQUEST -> ErrorType.INVALID_REQUEST
            else -> ErrorType.UNKNOWN
        }
    }
}