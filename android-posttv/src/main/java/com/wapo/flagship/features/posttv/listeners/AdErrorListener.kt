package com.wapo.flagship.features.posttv.listeners

import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.wapo.flagship.features.posttv.model.Video

class AdErrorListener(private val mListener: VideoListener, private val video: Video) : AdErrorEvent.AdErrorListener {
    override fun onAdError(adErrorEvent: AdErrorEvent) {
        mListener.onAdError(adErrorEvent.error, video)
    }
}