package com.wapo.flagship.features.audio

import com.wapo.android.commons.util.Logger

object AudioAdsFlag {
    private const val TAG = "AudioAdsFlag"

    //  Global frequency cap
    private var lastAdPlayed: Long? = null

    //  Audio frequency cap. Resets when player starts a new audio
    private var lastAdBreakPlayed: Long? = null

    fun shouldPlayAd(interval: Long, updateTimestamp: Boolean = true): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        val playAd =
            lastAdPlayed == null || currentTimeMillis >= lastAdPlayed!! + interval
        if (playAd && updateTimestamp) {
            lastAdPlayed = currentTimeMillis
        }
        Logger.d(TAG, "shouldPlayAd: $playAd ----- $lastAdPlayed")
        return playAd
    }

    fun shouldPlayAd(
        globalIntervalMs: Long?,
        adBreakIntervalMs: Long?,
    ): Boolean {
        val currentTimeMs = System.currentTimeMillis()
        val lastAdPlayed = this.lastAdPlayed
        val lastAdBreakPlayed = this.lastAdBreakPlayed
        val shouldPlayAd = when {
            globalIntervalMs == null && adBreakIntervalMs == null -> true
            globalIntervalMs != null && lastAdPlayed != null && currentTimeMs < lastAdPlayed + globalIntervalMs -> {
                Logger.d(TAG, "Should NOT play ad: Global ad was played ${(currentTimeMs - lastAdPlayed).div(1000)} seconds ago and minAdInterval is ${globalIntervalMs.div(1000)}s")
                false
            }

            adBreakIntervalMs != null && lastAdBreakPlayed != null && currentTimeMs < lastAdBreakPlayed + adBreakIntervalMs -> {
                Logger.d(TAG, "Should NOT play ad: AdBreak ad was played ${(currentTimeMs - lastAdBreakPlayed).div(1000)} seconds ago and minAdInterval is ${adBreakIntervalMs.div(1000)}s")
                false
            }

            else -> {
                Logger.d(TAG, "Should play ad: global ad played ${(currentTimeMs - (lastAdPlayed ?: 1)).div(1000)} seconds ago (minInterval=${globalIntervalMs?.div(1000)}) and ad break played  ${(currentTimeMs - (lastAdBreakPlayed ?: 1)).div(1000)} seconds ago (minInterval=${adBreakIntervalMs?.div(1000)})")
                true
            }
        }
        return shouldPlayAd
    }

    /**
     * Should be called when starting a new audio
     */
    fun resetLastAdBreakTimestamp() {
        lastAdBreakPlayed = null
    }

    fun updateTimestampsOnAdCompleted() {
        val currentTimeMs = System.currentTimeMillis()
        lastAdPlayed = currentTimeMs
        lastAdBreakPlayed = currentTimeMs
    }
}