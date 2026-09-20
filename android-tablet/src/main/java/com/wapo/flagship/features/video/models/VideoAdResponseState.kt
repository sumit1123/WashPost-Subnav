package com.wapo.flagship.features.video.models

/**
 * States to handle Video Ads
 */
sealed class VideoAdResponseState {
    /**
     * Default state when an ad request is yet to be made
     */
    object Uninitialized : VideoAdResponseState()

    /**
     * State when making an ad request
     */
    object Loading : VideoAdResponseState()

    /**
     * State when response is success
     */
    data class Success(
        val adResponse: VideoAdResponse,
    ) : VideoAdResponseState()

    /**
     * State when response is an error
     */
    object Error : VideoAdResponseState()
}
