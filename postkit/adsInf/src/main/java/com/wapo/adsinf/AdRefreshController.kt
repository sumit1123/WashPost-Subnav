package com.wapo.adsinf

import com.wapo.android.commons.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdRefreshController(
    private val adView: BannerAdView,
    private val scope: CoroutineScope,
) {
    private var refreshIntervalMs: Long? = null
    private val isActive
        get() = refreshIntervalMs.let { it != null && it > 0 }
    private var isVisible = false

    private var accumulatedVisibleTime = 0L
    private var lastVisibilityTimestamp = 0L
    private var lastRefreshAttempt = 0L

    private var currentAdId: String? = null
    private var hasImpression = false

    private var checkRefreshJob: Job? = null
    private var renderCount: Int = 1

    private fun start() {
        if (!isActive || checkRefreshJob != null) return
        checkRefreshJob = scope.launch {
            while (isActive) {
                delay(CHECK_REFRESH_INTERVAL)
                checkRefresh("timer")
            }
        }
        log("refresh loop started")
    }

    fun stop() {
        checkRefreshJob?.cancel()
        checkRefreshJob = null
        log("refresh loop stopped")
    }

    fun setRefreshIntervalMs(value: Long?) {
        if (value == null || value <= 0) {
            log("setRefreshIntervalMs called with null or invalid value: ${value}ms. Resetting controller")
            refreshIntervalMs = null
            resetControllerState()
            stop()
        } else {
            log("setRefreshIntervalMs called with: ${value}ms")
            refreshIntervalMs = value
        }
    }

    fun onAdLoadStarted(adId: String) {
        if (!isActive) return
        currentAdId = adId
        hasImpression = false
        resetTimer()

        log("Ad load started: $adId")

        start()
    }

    fun onAdLoaded(adId: String) {
        if (!isActive) return
        log("Ad loaded: $adId")
    }

    fun onAdFailed(adId: String) {
        if (!isActive) return
        log("Ad failed: $adId")
    }

    fun onAdImpression(adId: String) {
        if (!isActive) return
        if (adId == currentAdId) {
            hasImpression = true
            log("Ad impression: $adId")
        }
    }

    fun onVisibilityChanged(visible: Boolean) {
        if (visible == isVisible) return
        isVisible = visible

        if (!isActive) return
        val now = System.currentTimeMillis()
        if (visible) {
            lastVisibilityTimestamp = now
            log("became visible")
            checkRefresh("visibility_change")
            start()
        } else {
            accumulatedVisibleTime += now - lastVisibilityTimestamp
            log("became invisible (accumulated=${accumulatedVisibleTime}ms)")
            stop()
        }
    }

    private fun checkRefresh(source: String) {
        val now = System.currentTimeMillis()
        val visibleTime = getTotalVisibleTime(now)
        val refreshIntervalMs = this.refreshIntervalMs ?: return

        if (visibleTime < refreshIntervalMs) return
        if (!isVisible) return
        if (adView.isLoadingAd()) return
        if (now - lastRefreshAttempt < CHECK_REFRESH_INTERVAL) return

        triggerRefresh(source)
    }

    private fun triggerRefresh(source: String) {
        val now = System.currentTimeMillis()
        lastRefreshAttempt = now

        val adId = currentAdId
        if (adId != null && !hasImpression) {
            log("Drop-off detected (no impression): adId=$adId")
        }
        log("Refresh triggered (source=$source, visibleTime=${getTotalVisibleTime(now)}ms)")
        val config = adView.getCurrentAdConfig()
        if (config == null) {
            log("No AdConfig available, skipping refresh")
            return
        }
        renderCount = config.adRequestTargets.getRenderCount() + 1
        config.adRequestTargets.setRenderCount(renderCount)
        adView.loadAd(config)
    }

    private fun resetTimer() {
        accumulatedVisibleTime = 0L
        lastVisibilityTimestamp = if (isVisible) {
            System.currentTimeMillis()
        } else {
            0L
        }
    }

    private fun resetControllerState() {
        accumulatedVisibleTime = 0L
        lastVisibilityTimestamp = 0L
        lastRefreshAttempt = 0L
        currentAdId = null
        hasImpression = false
    }

    private fun getTotalVisibleTime(now: Long): Long {
        return if (isVisible) {
            accumulatedVisibleTime + (now - lastVisibilityTimestamp)
        } else {
            accumulatedVisibleTime
        }
    }

    private fun log(message: String) {
        Logger.d(TAG, "[AdView@${adView.hashCode()}] $message")
    }

    companion object {
        private const val TAG = "AdRefreshController"
        private const val CHECK_REFRESH_INTERVAL = 1000L
    }
}