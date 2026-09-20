package com.wapo.adsinf.sdk.nimbus

import com.adsbynimbus.NimbusError
import com.adsbynimbus.openrtb.request.Format
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdError
import com.wapo.adsinf.models.AdError.ErrorType

object NimbusUtils {

    fun convertToNimbusFormat(size: AdDimension): Format? {
        return when (size) {
            AdDimension.Medium -> Format.LETTERBOX
            AdDimension.Fluid -> null
            AdDimension.Tall -> Format.HALF_SCREEN
            AdDimension.Banner728x90 -> Format.LEADERBOARD
            else -> Format(size.w, size.h)
        }
    }

    // InitialFormat (first element in the returned List) must be set here and it MUST be one of the
    // pre-defined case sizes != .letterbox (e.g. .INTERSTITIAL_LAND, .INTERSTITIAL_PORT,
    // .HALF_SCREEN etc) if we want nimbus to fill with any non letterbox format
    fun convertToNimbusFormatList(sizes: List<AdDimension>): List<Format> {
        val result = mutableListOf<Format>()
        val initialFormat = when {
            sizes.contains(AdDimension.Banner620x250) || sizes.contains(AdDimension.Banner970x250) -> Format.INTERSTITIAL_LAND
            sizes.contains(AdDimension.Banner728x90) -> Format.LEADERBOARD
            sizes.contains(AdDimension.Tall) -> Format.HALF_SCREEN
            else -> Format.LETTERBOX
        }
        result.add(initialFormat)
        sizes.forEach { size ->
            convertToNimbusFormat(size)?.let { result.add(it) }
        }
        return result.distinct()
    }

    fun NimbusError.toAdError(): AdError {
        return AdError(
            type = getErrorType(this),
            message = this.message ?: this.toString(),
        )
    }

    fun getErrorType(error: NimbusError): ErrorType {
        return when (error.errorType) {
            NimbusError.ErrorType.NOT_INITIALIZED -> ErrorType.SDK_NOT_INITIALIZED
            NimbusError.ErrorType.NO_BID -> ErrorType.NO_FILL
            NimbusError.ErrorType.NETWORK_ERROR -> ErrorType.NETWORK_ERROR
            NimbusError.ErrorType.RENDERER_ERROR -> ErrorType.RENDER_ERROR
            else -> ErrorType.UNKNOWN
        }
    }
}