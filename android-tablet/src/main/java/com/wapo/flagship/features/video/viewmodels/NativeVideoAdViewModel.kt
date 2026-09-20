package com.wapo.flagship.features.video.viewmodels

import android.app.Application
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeCustomFormatAd
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor.Companion.headers
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.posttv.ExoPlayerCache
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.features.video.models.VerticalVideoAdItem
import com.wapo.flagship.features.video.models.VideoAdItem
import com.wapo.flagship.features.video.models.VideoAdResponse
import com.wapo.flagship.features.video.models.VideoAdResponseState
import com.wapo.flagship.features.wpvideos.fragments.WatchVideoFragment
import com.wapo.flagship.features.wpvideos.models.WatchVideoAdItem
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * ViewModel class for the Video Ads
 */
@HiltViewModel
class NativeVideoAdViewModel @Inject constructor(
    val app: Application,
    private val adService: AdService,
) : ViewModel() {
    private val tag = NativeVideoAdViewModel::class.java.simpleName

    /**
     * Items list for the recyclerview adapter can use.
     */
    val itemsList = mutableListOf<Any>()

    /**
     * SparseArray to hold the pre calculated ad positions and the ad items
     */
    private val adItems = SparseArray<VideoAdItem>()

    /**
     * SparseArray to hold the ad positions and the ad response states
     */
    private val adResponseStates = SparseArray<MutableLiveData<VideoAdResponseState>>()

    /**
     * SparseArray to map actual to expected ad positions based on VideoAdResponseState of ads.
     * actual: ad positions once ads are returned and injected.
     * expected: predetermined ad positions based on config values.
     */
    private val finalAdPosition = SparseArray<Int>()

    /**
     * Event live data to be fired when [itemsList] is ready after ads are injected.
     */
    private val _itemsListReadyEvent = LiveEvent<Any>()

    /**
     * Event live data to be fired when ui timer timed out.
     */
    private val _timerCompletion = LiveEvent<Boolean>()

    private var customTargetsMap: HashMap<String, List<String>>? = null

    private var waitJob: Job? = null

    /**
     * MediatorLiveData to observe whichever event (_timerCompletion or _itemsListReadyEvent)
     * occurs first.
     */
    private val _waitCompletion =
        MediatorLiveData<Boolean>().apply {
            addSource(_timerCompletion) {
                Logger.d(
                    tag,
                    "_waitCompletion, _timerCompletion, timer=$it, listReady=${_itemsListReadyEvent.value != null}",
                )
                value = it == true || _itemsListReadyEvent.value != null
            }
            addSource(_itemsListReadyEvent) {
                Logger.d(
                    tag,
                    "_waitCompletion, _itemsListReadyEvent, timer=${_timerCompletion.value}, listReady=${it != null}",
                )
                value = it != null || _timerCompletion.value == true
            }
        }
    val waitCompletion: LiveData<Boolean> = _waitCompletion

    private var cleared: Boolean = false

    init {
        viewModelScope.launch {
            adService.adsMode.collect { mode ->
                if (mode is AdsModel.Disabled) {
                    removeAds()
                }
            }
        }
    }

    private fun removeAds() {
        if (itemsList.any { it is VideoAdItem }) {
            val iterator = itemsList.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() is VideoAdItem) {
                    iterator.remove()
                }
            }
            finalAdPosition.clear()
            _itemsListReadyEvent.value = Any() // Trigger UI refresh
        }
    }

    /**
     * Method to prepare ad positions and then load them and insert them to the [itemsList].
     *
     * firstItemDelay: First ad position to insert when default ad position is opened directly.
     * adItemInterval: Interval to maintain number of videos between ads.
     */
    fun injectAds(
        firstItemDelay: Int,
        adItemInterval: Int,
        uiTimeoutMillis: Long,
        itemPosition: Int,
        sourceScreen: String?
    ) {
        if (adService.currentAdsMode is AdsModel.Disabled) return

        release()

        val firstAdItemIndex =
            if (itemPosition > 0 && itemPosition % adItemInterval == 0) {
                firstItemDelay
            } else {
                adItemInterval
            }

        var adsCount = 0
        var nextAdPos = firstAdItemIndex
        while (nextAdPos >= 0 && nextAdPos <= itemsList.size + adsCount) {
            val adItem = if (sourceScreen == WatchVideoFragment.WP_VIDEO_BUNDLE_NAME) {
                WatchVideoAdItem(nextAdPos)
            } else {
                VerticalVideoAdItem(nextAdPos)
            }
            adItems.put(nextAdPos, adItem)
            adResponseStates.put(nextAdPos, MutableLiveData(VideoAdResponseState.Uninitialized))
            adsCount++
            loadNativeCustomFormatAd(adItem, nextAdPos)
            if (adItemInterval <= 0) break
            nextAdPos += adItemInterval + 1
        }

        // Start ui timeout timer as soon as requests are fired.
        if (adItems.size() > 0) {
            startAdTimeoutTimer(uiTimeoutMillis)
        }
    }

    /**
     * to get the new item position once after injecting the ads
     */
    fun getItemPositionAfterAddingAds(
        items: MutableList<Any>,
        itemPosition: Int,
    ): Int {
        var videoItemsCount = -1
        items.forEachIndexed { index, item ->
            if (item is Video) {
                videoItemsCount++
            }
            if (videoItemsCount == itemPosition) {
                return index
            }
        }
        return 0
    }

    /**
     * Method to load NativeCustomFormatAd
     */
    private fun loadNativeCustomFormatAd(
        adItem: VideoAdItem,
        itemPosition: Int,
    ) {
        if (adResponseStates.get(itemPosition) == null) {
            return
        }

        val adState = adResponseStates.get(itemPosition).value
        if (adState is VideoAdResponseState.Success) {
            adState.adResponse.nativeCustomFormatAd?.destroy()
        }
        adResponseStates.get(itemPosition).value = VideoAdResponseState.Loading

        val videoOptions =
            VideoOptions
                .Builder()
                .setStartMuted(false)
                .build()

        val adOptions =
            NativeAdOptions
                .Builder()
                .setVideoOptions(videoOptions)
                .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
                .build()

        var adLoaderBuilder = AdLoader.Builder(app, adItem.adUnitId)

        adLoaderBuilder =
            adLoaderBuilder
                .forCustomFormatAd(
                    adItem.templateId,
                    { ad: NativeCustomFormatAd ->
                        Logger.d(tag, "loadNativeCustomFormatAd()")
                        // If this callback occurs after the activity is destroyed, we must call
                        // destroy and return or we may get a memory leak.
                        if (cleared) {
                            ad.destroy()
                            return@forCustomFormatAd
                        }
                        val videoUrl = ad.getText("VideoURL")?.toString()
                        val ctaButtonText = ad.getText("CTAButtonText")?.toString()
                        val ctaButtonHexColor = ad.getText("CTAButtonHexColor")?.toString()
                        val impressionPixel = ad.getText("ImpressionPixel")?.toString()
                        val videoPlayPixel = ad.getText("VideoPlayPixel")?.toString()
                        val videoPausePixel = ad.getText("VideoPausePixel")?.toString()
                        val videoCompletionPixel = ad.getText("VideoCompletionPixel")?.toString()
                        val gamCreativeId = ad.getText("GAMCreativeID")?.toString()
                        val gamLineItemId = ad.getText("GAMLineItemID")?.toString()
                        val impressionPixels =
                            ArrayList<String>().apply {
                                // Add impressionPixel and other impressionPixels when impressionPixel is not null.
                                if (impressionPixel != null) {
                                    add(impressionPixel)
                                    // Add remaining impressionPixels (impressionPixel_2, impressionPixel_3, etc.,)
                                    var pixelNum = 2
                                    do {
                                        val impressionPixelK = ad.getText("ImpressionPixel_$pixelNum")?.toString()
                                        if (impressionPixelK != null) add(impressionPixelK) else break
                                        pixelNum++
                                    } while (true)
                                }
                            }
                        Logger.d(
                            tag,
                            "loadNativeCustomFormatAd(): impression pixels count=${impressionPixels.size}, videoUrl=$videoUrl",
                        )
                        val response =
                            VideoAdResponse(
                                videoUrl,
                                ctaButtonText,
                                ctaButtonHexColor,
                                impressionPixels,
                                videoPlayPixel,
                                videoPausePixel,
                                videoCompletionPixel,
                                gamCreativeId,
                                gamLineItemId,
                                ad,
                            )
                        if (!videoUrl.isNullOrEmpty()) {
                            ExoPlayerCache.cacheVideo(videoUrl)
                        }

                        adResponseStates.get(itemPosition).value = VideoAdResponseState.Success(response)
                        Logger.d(tag, "loadNativeCustomFormatAd(): inserted ad at $itemPosition")
                        postListReadyEventIfReady()
                    },
                    // Should pass null here to handle click event by the sdk.
                    null,
                )
                // Methods in the NativeAdOptions.Builder class can be
                // used here to specify individual options settings.
                .withNativeAdOptions(adOptions)

        val adLoader =
            adLoaderBuilder
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            val error =
                                "NativeCustomFormatAd Error,  item_pos=$itemPosition, domain=${loadAdError.domain}, error_code=${loadAdError.code}, error_msg=${loadAdError.message}"
                            Logger.e(tag, "loadNativeCustomFormatAd(): onAdFailedToLoad(), error=$error")
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("NativeCustomFormatAd Error")
                                    setModule(LogModules.VIDEO_ADS)
                                    setErrorMessage(loadAdError.message)
                                    setErrorCode(loadAdError.code)
                                    set("item_pos", itemPosition)
                                    set("domain", loadAdError.domain)
                                }.run {
                                    RemoteLog.e(app, build())
                                }
                            viewModelScope.launch(Dispatchers.Main) {
                                adResponseStates.get(itemPosition).value = VideoAdResponseState.Error
                                postListReadyEventIfReady()
                            }
                        }

                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            Logger.d(tag, "loadNativeCustomFormatAd(): onAdLoaded()")
                        }

                        override fun onAdOpened() {
                            super.onAdOpened()
                            Logger.d(tag, "loadNativeCustomFormatAd(): onAdOpened()")
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            Logger.d(tag, "loadNativeCustomFormatAd(): onAdClicked()")
                        }
                    },
                ).build()

        AdManagerAdRequest.Builder().also {
            setContextualValues(it)
            adLoader.loadAd(it.build())
        }
    }

    fun getAdState(itemPosition: Int): LiveData<VideoAdResponseState>? {
        if (itemPosition == RecyclerView.NO_POSITION) return null
        val expectedAdPosition = finalAdPosition[itemPosition]
        return adResponseStates.get(expectedAdPosition)
    }

    fun getAdResponse(itemPosition: Int): VideoAdResponse? {
        if (itemPosition == RecyclerView.NO_POSITION) return null
        val expectedAdPosition = finalAdPosition[itemPosition]
        return (adResponseStates.get(expectedAdPosition).value as? VideoAdResponseState.Success)?.adResponse
    }

    /**
     * to post ListReadyEvent once [itemsList] is ready
     */
    @Synchronized
    private fun postListReadyEventIfReady() {
        // Ignore events once timed out.
        if (!isTimerRunning()) return
        // All ads should be returned before posting ListReady Event
        adResponseStates.forEach { _, value ->
            if (value.value !is VideoAdResponseState.Success && value.value !is VideoAdResponseState.Error) {
                return
            }
        }
        Logger.d(tag, "postListReadyEventIfReady(), Posting ListReady Event")
        postListReadyEvent()
    }

    /**
     * Posts list ready event to start playing videos.
     * This gets called once all ad requests are returned within timeout.
     */
    private fun postListReadyEvent() {
        // Post an event to start playing videos.
        if (_itemsListReadyEvent.value == null) {
            _itemsListReadyEvent.value = Any()
        }
    }

    /**
     * Method to insert successful ads into the itemsList. It also adjusts ad positions based on the
     * successful ads as positions are predetermined based on the config values but they should change
     * based on the ad response states.
     */
    fun insertSuccessfulAdsAndUpdateFinalAdPositions() {
        if (adService.currentAdsMode is com.wapo.adsinf.models.AdsModel.Disabled) {
            val iterator = itemsList.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() is VideoAdItem) {
                    iterator.remove()
                }
            }
            return
        }

        var indexAdjustment = 0
        // Clean previous values in case this method gets called more than once.
        // But there are no such cases now.
        finalAdPosition.clear()
        val iterator = itemsList.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next is VideoAdItem) {
                iterator.remove()
            }
        }
        adResponseStates.forEach { expectedAdPosition, responseState ->
            if (responseState.value is VideoAdResponseState.Success) {
                val actualPosition = expectedAdPosition - indexAdjustment
                itemsList.add(actualPosition, adItems.get(expectedAdPosition))
                finalAdPosition.put(actualPosition, expectedAdPosition)
                Logger.d(
                    tag,
                    "insertSuccessfulAdsAndUpdateFinalAdPositions(), actualPos=$actualPosition, expectedPos=$expectedAdPosition",
                )
            } else {
                indexAdjustment++
            }
        }
    }

    /**
     * To send pixel or impression urls
     */
    fun sendPixelTrackingRequest(
        trackingUrl: String?,
        videoUrl: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            sendPixelTrackingRequestInBackground(trackingUrl, videoUrl)
        }
    }

    private fun sendPixelTrackingRequestInBackground(
        trackingUrl: String?,
        videoUrl: String?,
    ) {
        if (trackingUrl.isNullOrEmpty()) return
        var httpURLConnection: HttpURLConnection? = null
        try {
            val url = URL(trackingUrl)
            httpURLConnection = url.openConnection() as HttpURLConnection
            httpURLConnection.requestMethod = "GET"
            for (header in headers) {
                httpURLConnection.setRequestProperty(header.key, header.value)
            }
            if (httpURLConnection.responseCode == 200) {
                Logger.d(tag, "Successfully tracked tracking url=$trackingUrl")
            } else {
                Logger.d(tag, "Error response in handling tracking url=$trackingUrl")
                EventLog
                    .Builder()
                    .apply {
                        setMessage("NativeCustomFormatAd Tracking Url Error Response")
                        setModule(LogModules.VIDEO_ADS)
                        set("tracking_url", trackingUrl)
                        set("video_url", videoUrl)
                    }.run {
                        RemoteLog.e(app, build())
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e(tag, "Exception in handling tracking url=$trackingUrl")
            EventLog
                .Builder()
                .apply {
                    setMessage("NativeCustomFormatAd Tracking Url Error")
                    setModule(LogModules.VIDEO_ADS)
                    setErrorMessage(e.message)
                    set("tracking_url", trackingUrl)
                    set("video_url", videoUrl)
                }.run {
                    RemoteLog.e(app, build())
                }
        } finally {
            httpURLConnection?.disconnect()
        }
    }

    private fun release() {
        adResponseStates.forEach { _, value ->
            (value.value as? VideoAdResponseState.Success)?.let { adState ->
                adState.adResponse.nativeCustomFormatAd?.destroy()
            }
        }
        adResponseStates.clear()
        adItems.clear()
        finalAdPosition.clear()
        customTargetsMap?.clear()
    }

    /**
     * Sets all contextual and targeting values to the [AdManagerAdRequest.Builder]
     * All custom targeting values should be added to [customTargetsMap] to be added to the request.
     */
    private fun setContextualValues(adRequestBuilder: AdManagerAdRequest.Builder) {
        addAdOpsTestValues()
        addAdSubscriptionStatus()
        // Set content url
        adRequestBuilder.setContentUrl(app.resources.getString(R.string.ads_default_content_url))
        // set custom targeting values
        customTargetsMap?.forEach { adRequestBuilder.addCustomTargeting(it.key, it.value) }
    }

    /**
     * Sets custom params value from "Test Options - Ads" for Ad Ops testing (debug & prod).
     * Sets here as long as "Test options - Ads" options are enabled and value is not empty in settings.
     */
    private fun addAdOpsTestValues() {
        if (AppPreferences.isTestAdsEnabled()) {
            val value = AppPreferences.getTestAdsValue().trim()
            if (value.isNotEmpty()) {
                addCustomAdKeyValues("kw", value.split(","))
            }
        }
    }

    private fun addAdSubscriptionStatus(){
        val status = PaywallService.getConnector().adSubscriptionStatus
        addCustomAdKeyValues("sub=", listOf(status))
    }

    /**
     * Creates custom params map if it is called for the first time and sets with the given
     * key and list of values.
     * @param key
     * @param values
     */
    private fun addCustomAdKeyValues(
        key: String,
        values: List<String>,
    ) {
        if (customTargetsMap == null) {
            customTargetsMap = java.util.HashMap()
        }
        customTargetsMap?.put(key, values)
    }

    /**
     * Ad Item and the next video item should have the same positions in the tracking calls.
     */
    fun getTrackingPosition(adapterPosition: Int): Int {
        var trackingPosition = 0
        var adsCount = 0
        for (i in 0..adapterPosition) {
            trackingPosition = i + 1 - adsCount
            if (itemsList[i] is VideoAdItem) adsCount++
        }
        return trackingPosition
    }

    private fun startAdTimeoutTimer(uiTimeoutMillis: Long) {
        if (waitJob == null) {
            _timerCompletion.value = false
            waitJob =
                viewModelScope.launch(Dispatchers.Default) {
                    delay(uiTimeoutMillis)
                    Logger.d(tag, "startAdTimeoutTimer(), Timed out Ended")
                    _timerCompletion.postValue(true)
                }
        }
    }

    @Synchronized
    fun stopAdTimeoutTimer() {
        Logger.d(tag, "stopAdTimeoutTimer()")
        waitJob?.cancel()
        waitJob = null
    }

    fun isTimerRunning(): Boolean = waitJob != null

    override fun onCleared() {
        release()
        cleared = true
        super.onCleared()
    }
}
