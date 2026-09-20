package com.wapo.flagship.features.audio.ads.util

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.wapo.flagship.features.audio.ads.model.AudioAdConfig
import com.wapo.flagship.features.audio.service2.media.extensions.adsConfig
import com.wapo.flagship.features.audio.service2.media.extensions.contentMediaId
import com.wapo.flagship.features.audio.service2.media.extensions.contentMediaListIds
import com.wapo.flagship.features.audio.service2.media.extensions.isAd

object AdPlayerUtils {
    val Player.isPlayingAd2: Boolean
        get() = this.currentMediaItem?.mediaMetadata?.isAd == true

    inline fun MediaItem.Builder.putExtra(
        baseMetadata: MediaMetadata?,
        setDataFn: Bundle.() -> Unit,
    ): MediaItem.Builder {
        val extras = baseMetadata?.extras
        if (extras != null) {
            extras.setDataFn()
            setMediaMetadata(baseMetadata)
        } else {
            val bundle = Bundle().apply { setDataFn() }
            val metadata = (baseMetadata?.buildUpon() ?: MediaMetadata.Builder())
                .setExtras(bundle)
                .build()
            setMediaMetadata(metadata)
        }
        return this
    }

    val MediaItem.isAd: Boolean
        get() = this.mediaMetadata.isAd == true

    val MediaItem.adsConfig: AudioAdConfig?
        get() = this.mediaMetadata.adsConfig

    fun isAdForMediaItemList(
        adMediaItem: MediaItem,
        contentMediaItemList: List<MediaItem>,
        startIndex: Int
    ): Boolean {
        val contentMediaItem = contentMediaItemList.getOrNull(startIndex) ?: return false
        return adMediaItem.isAd && !contentMediaItem.isAd &&
                adMediaItem.mediaMetadata.contentMediaId == contentMediaItem.mediaId &&
                adMediaItem.mediaMetadata.contentMediaListIds == contentMediaItemList.map { it.mediaId }
    }
}