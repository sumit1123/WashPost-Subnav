package com.wapo.adsinf

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.wapo.adsinf.interfaces.AdLoadCallback
import com.wapo.adsinf.interfaces.AdUiEventsListener
import com.wapo.adsinf.interfaces.BannerAdRenderer
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdError
import com.wapo.adsinf.models.AdRequest
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdSlotType
import com.wapo.adsinf.models.AdLoadSession
import com.wapo.adsinf.sdk.google.GoogleBannerAdRenderer
import com.wapo.adsinf.sdk.nimbus.NimbusBannerAdRenderer
import com.wapo.adsinf.utils.AdsUtil
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.Utils
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class BannerAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), AdUiEventsListener {
    private var currentAdConfig: AdConfig? = null
    private var lastReservedSize: Size? = null
    private var renderer: BannerAdRenderer? = null
    private val coroutineScope by lazy {
        findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope ?: MainScope()
    }
    private var loadJob: Job? = null
    private var updateVisibilityJob: Job? = null
    private val adsConfig = ConfigManager.getInstance().config.adsConfig.bannersConfig
    private val refreshController: AdRefreshController by lazy {
        AdRefreshController(
            adView = this@BannerAdView,
            scope = coroutineScope,
        ).apply {
            setRefreshIntervalMs(adsConfig.autoRefreshIntervalInSeconds?.times(1000L))
        }
    }

    /**
     * Loads an ad using the provided configuration.
     * The method delegates ad loading and fallback orchestration to [AdManager]. This
     * [com.wapo.adsinf.BannerAdView] itself does not perform provider selection.
     */
    fun loadAd(config: AdConfig) {
        loadJob?.cancel()
        currentAdConfig = config
        prepareContainerForAd(config)
        loadJob = coroutineScope.launch {
            AdManager.getInstance().loadBannerAd(this@BannerAdView, config)
        }
    }

    /**
     * Renders an ad using a prepared [AdRequest]. The view selects the appropriate [BannerAdRenderer]
     * implementation (Google or Nimbus) and attempts to render the ad/[AdRequest].
     * This method is called by [AdManager] once a valid [AdRequest] has been created for a specific
     * provider. It can also be called by any other component, if the ad provider is known and we
     * do not want any fallback logic.
     */
    fun loadAd(adRequest: AdRequest, adLoadSession: AdLoadSession, callback: AdLoadCallback) {
        prepareContainerForAd(adRequest.adConfig)
        renderer?.release(this)
        removeAllViews()
        renderer = when (adRequest) {
            is AdRequest.Google -> GoogleBannerAdRenderer()
            is AdRequest.Nimbus -> NimbusBannerAdRenderer()
        }
        if (adLoadSession.attempts.isEmpty() || adLoadSession.attempts.lastOrNull()?.adResponse != null) {
            adLoadSession.createAttempt()
        }

        val adId = UUID.randomUUID().toString()
        updateAdRefreshIntervalMs(adRequest.adLoaderConfig.loaderType)
        refreshController.onAdLoadStarted(adId)
        renderer?.load(
            this,
            adRequest,
            adLoadSession,
            object : AdLoadCallback {
                override fun onAdLoaded() {
                    callback.onAdLoaded()
                    refreshController.onAdLoaded(adId)
                }

                override fun onAdFailed(error: AdError) {
                    callback.onAdFailed(error)
                    refreshController.onAdFailed(adId)
                }

                override fun onAdImpression() {
                    callback.onAdImpression()
                    refreshController.onAdImpression(adId)
                }

            }
        )
    }

    private fun updateAdRefreshIntervalMs(loader: AdLoaderType) {
        val newAdRefreshIntervalMs = when (loader) {
            AdLoaderType.NIMBUS -> (adsConfig.nimbus.refreshIntervalInSeconds
                ?: adsConfig.autoRefreshIntervalInSeconds)?.times(1000L)
            AdLoaderType.GOOGLE -> adsConfig.autoRefreshIntervalInSeconds?.times(1000L)
        }
        refreshController.setRefreshIntervalMs(newAdRefreshIntervalMs)
    }

    override fun addView(child: View?) {
        val params = child?.layoutParams
        val newParams = if (params is LayoutParams) {
            params.apply { gravity = Gravity.CENTER }
        } else {
            LayoutParams(
                params?.width ?: LayoutParams.WRAP_CONTENT,
                params?.height ?: LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        super.addView(child, newParams)
    }

    private fun prepareContainerForAd(config: AdConfig) {
        val configSpaceToReserve = getSpaceToReserve(config)
        if (lastReservedSize == configSpaceToReserve) {
            Logger.d(TAG, "container already prepared!")
            return
        }
        reserveAdSpaceInAdContainer(config)
        lastReservedSize = configSpaceToReserve
        if (!Utils.isConnectedOrConnecting(context)) {
            Logger.d(TAG, "No network. Showing offline ad!")
            showOfflineAd(config)
        }
    }

    private fun reserveAdSpaceInAdContainer(config: AdConfig) {
        val adDimension = getSpaceToReserve(config)
        if (adDimension != null) {
            val displayMetrics = context.resources.displayMetrics
            val widthInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                adDimension.width.toFloat(),
                displayMetrics
            ).toInt()
            val heightInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                adDimension.height.toFloat(),
                displayMetrics
            ).toInt()
            this.minimumWidth = widthInDp
            this.minimumHeight = heightInDp
            Logger.d(
                TAG,
                "ReserveAdSpaceInContainer: spaceToReserve=$adDimension, (minWidth,minHeight)=($widthInDp,$heightInDp)"
            )
        }
    }

    /**
     * Passing the minimum height to reserve (300).
     * The short ad slot can support either 250 or 300 height Fluid ads.
     * To avoid resizing/jumping each time, we statically reserve 300 height at minimum.
     */
    private fun getSpaceToReserve(config: AdConfig): Size? {
        val minHeight = if (config.containerMinHeight > 0) {
            config.containerMinHeight
        } else {
            AdsUtil.MIN_HEIGHT_TO_RESERVE_IN_AD_CONTAINER
        }
        return getLargestDimensions(config.adDimensions, minHeight)
    }

    fun getLargestDimensions(adDimensions: List<AdDimension>, minHeight: Int): Size? {
        if (adDimensions.isEmpty()) return null

        var largestWidth = Integer.MIN_VALUE
        var largestHeight = minHeight
        for (adDimen in adDimensions) {
            if (adDimen.w > largestWidth) {
                largestWidth = adDimen.w
            }
            if (adDimen.h > largestHeight) {
                largestHeight = adDimen.h
            }
        }
        return Size(largestWidth, largestHeight)
    }

    override fun showLoading() {
        val currentProgressBar = getTag(PROGRESS_TAG_KEY) as? ProgressBar?
        if (currentProgressBar != null) {
            currentProgressBar.isVisible = true
            return
        }
        val progressBar =
            ProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
                isIndeterminate = true
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            }
        setTag(PROGRESS_TAG_KEY, progressBar)
        addView(progressBar)
    }

    override fun hideLoading() {
        val progressBar = getTag(PROGRESS_TAG_KEY) as? ProgressBar?
        if (progressBar != null) {
            removeView(progressBar)
            setTag(PROGRESS_TAG_KEY, null)
        }
    }

    override fun showOfflineAd(config: AdConfig) {
        removeAllViews()
        addView(getOfflineAdView(config))
    }

    private fun getOfflineAdView(config: AdConfig): View {
        val drawable = ContextCompat.getDrawable(
            context,
            config.offlineViewResId ?: when (config.adSlotType) {
                AdSlotType.TALL -> R.drawable.default_ad_background
                else -> R.drawable.bigbox_ad_background_bitmap
            }
        )
        return ImageView(context).apply {
            setImageDrawable(drawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
    }

    override fun renderAdView(view: View) {
        removeAllViews()
        addView(view)
    }

    override fun resizeAdSlot() {
        this.minimumHeight = 0
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkVisibility()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        refreshController.onVisibilityChanged(false)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        checkVisibility()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        checkVisibility()
    }

    fun checkVisibility() {
        updateVisibilityJob?.cancel()
        updateVisibilityJob = coroutineScope.launch {
            delay(VISIBILITY_DEBOUNCE_MS)
            val visibleRatio = computeVisibleRatio()
            val isVisible = visibleRatio >= MIN_VISIBLE_RATIO
            Logger.d(TAG, "[AdView@${hashCode()}] visibleRatio=$visibleRatio, isVisible=$isVisible")
            refreshController.onVisibilityChanged(isVisible)
        }
    }

    private fun computeVisibleRatio(): Float {
        if (!isAttachedToWindow) return 0f
        if (!isShown) return 0f
        if (width == 0 || height == 0) return 0f

        val visibleRect = Rect()
        if (!getGlobalVisibleRect(visibleRect)) return 0f

        val visibleArea = visibleRect.width() * visibleRect.height()
        val totalArea = width * height
        if (totalArea <= 0) return 0f

        return visibleArea.toFloat() / totalArea.toFloat()
    }

    fun isLoadingAd(): Boolean {
        return loadJob?.isActive == true
    }

    fun getCurrentAdConfig(): AdConfig? = currentAdConfig

    fun release() {
        Logger.d(TAG, "[AdView@${hashCode()}]: release")
        currentAdConfig = null
        updateVisibilityJob?.cancel()
        updateVisibilityJob = null
        loadJob?.cancel()
        loadJob = null
        refreshController.stop()
        lastReservedSize = null
        renderer?.release(this)
        renderer = null
        removeAllViews()
    }

    companion object {
        private const val TAG = "AdView"
        private val PROGRESS_TAG_KEY = R.id.progress_spinner_bar
        private const val MIN_VISIBLE_RATIO = 0.5f

        //  Debounce window to filter noisy visibility changes caused by layout passes / scrolling
        private const val VISIBILITY_DEBOUNCE_MS = 100L
    }
}