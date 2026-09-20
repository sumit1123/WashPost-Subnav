package com.wapo.adsinf.sdk.amazon

import com.wapo.adsinf.models.AdError
import com.wapo.adsinf.models.AdError.ErrorType

object AmazonAdsUtils {

    fun com.amazon.device.ads.AdError.toAdError(): AdError {
        return AdError(
            type = getErrorType(this),
            message = this.message,
        )
    }

    fun getErrorType(error: com.amazon.device.ads.AdError): ErrorType {
        return when (error.code) {
            com.amazon.device.ads.AdError.ErrorCode.NO_FILL -> ErrorType.NO_FILL
            com.amazon.device.ads.AdError.ErrorCode.NETWORK_ERROR -> ErrorType.NETWORK_ERROR
            com.amazon.device.ads.AdError.ErrorCode.NETWORK_TIMEOUT -> ErrorType.TIMEOUT
            com.amazon.device.ads.AdError.ErrorCode.REQUEST_ERROR -> ErrorType.INVALID_REQUEST
            else -> ErrorType.UNKNOWN
        }
    }
}