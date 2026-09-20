package com.wapo.flagship.features.audio.config

import android.app.Activity
import android.content.Context
import com.wapo.android.commons.logs.EventLog
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.ads.model.AdRequestContext
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.models.MediaItemData
import com.wapo.flagship.features.audio.playlist.AudioTrackingInfo
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import okhttp3.RequestBody
import rx.Observable

interface AudioProvider {

    fun onPodcastEvent(type: EventType, mediaItemData: MediaItemData?, value: Any? = null, audioTracker: AudioTracker? = null, duration: Long? = null, progressThreshold: Int = 0)

    fun getCurrentActivity(): Activity?

    fun getAudioApiBaseUrl(): String

    fun saveUserPersoPodConfig(body: RequestBody)

    fun getAudioAdInterval(): Long

    fun getDisabledAudioUrls(): List<String>

    fun getThumbnailImageRequestURL(url: String?) : String?

    fun getFullWidthImageRequestURL(url: String?) : String?

    fun getImageLoader(): AnimatedImageLoader

    fun isRainbow(): Boolean

    fun openSubscriptionLink(url: String, context: Context)

    fun openArticles(
        context: Context?,
        appSection: String,
        url: String,
        sectionName: String
    )

    fun debugLog(context: Context, eventLogBuilder: EventLog.Builder)

    fun onError(context: Context, eventLogBuilder: EventLog.Builder)

    fun getImageResizerUrlForAuto(url: String?): String?

    enum class EventType {
        ON_PLAY_STARTED,
        ON_PERCENTAGE_PLAYED,
        ON_PROGRESS,
        ON_SUBSCRIBE,
        ON_ERROR
    }

    fun getPodcastItems(): Observable<List<AudioMediaConfig>>

    fun trackCarPlayOpenEvent(section: String, trackingInfo: AudioTrackingInfo? = null, isTopRibbon: Boolean = false)

    fun trackPersoEvents(avName: String?, touchpoint: String?, miscellany: String?, avTags: String?, id: String?)

    fun trackPersoShare(avName: String?, touchpoint: String?, miscellany: String?)

    fun trackPersoMenuOpen(avName: String?, touchpoint: String?, miscellany: String?)

    fun createPodcastTracker(duration: Long, persoPodTrackingInfo: Pair<PersoPodTrackingInfo?, PersoPodTrackingInfo?>?): AudioTracker

    fun getUserHeaders() : HashMap<String, String>

    fun shouldSuppressAds(): Boolean

    suspend fun getPersonalizedPodcasts(): AudioMediaConfig?

    suspend fun generatePersonalizedPodcast(): AudioMediaConfig?

    fun getUserPrivacyConsentForAds(context: Context): AdRequestContext.UserPrivacyConsent

    suspend fun sharePodcast(context: Context, podcastId: String)
}
