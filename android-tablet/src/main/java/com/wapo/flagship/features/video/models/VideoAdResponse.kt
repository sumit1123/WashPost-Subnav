package com.wapo.flagship.features.video.models

import com.google.android.gms.ads.nativead.NativeCustomFormatAd

/**
 * Class to hold values from [NativeCustomFormatAd]
 */
data class VideoAdResponse(
    val videoUrl: String?,
    val ctaButtonText: String?,
    val ctaButtonHexColor: String?,
    val impressionPixels: ArrayList<String>,
    val videoPlayPixel: String?,
    val videoPausePixel: String?,
    val videoCompletionPixel: String?,
    val gamCreativeId: String?,
    val gamLineItemId: String?,
    val nativeCustomFormatAd: NativeCustomFormatAd?,
)
