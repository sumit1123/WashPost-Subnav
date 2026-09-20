package com.wapo.adsinf.interfaces

import com.wapo.adsinf.models.AdError

interface AdLoadCallback {
    fun onAdLoaded()
    fun onAdFailed(error: AdError)
    fun onAdImpression() {}
}