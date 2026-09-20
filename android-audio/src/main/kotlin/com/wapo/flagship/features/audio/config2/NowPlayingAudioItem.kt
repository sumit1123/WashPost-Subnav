/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio.config2

import com.wapo.flagship.features.audio.models.MediaItemData
import com.wapo.flagship.features.audio.models.AudioPlaybackState

/**
 * [ClassicAudioManager2] constructs a live data object of NowPlayingAudioItem based on the player
 * playback and metadata states of currently playing item.
 *
 * playlistId: unique id of the playlist. It is same as the [AudioMediaConfigList] id.
 * audioMediaConfigItemIndex: index from [AudioMediaConfigList] list.
 * audioMediaConfig: [AudioMediaConfig] object
 * nowPlayingItemIndex: index from [MediaItemData]. audioMediaConfigItemIndex and this index can be
 * different.
 * mediaItemData: [MediaItemData] object
 * audioPlaybackState: [AudioPlaybackState]
 */
data class NowPlayingAudioItem(
    val playlistId: String?,
    val audioMediaConfigItemIndex: Int,
    val audioMediaConfig: AudioMediaConfig?,
    val nowPlayingItemIndex: Int,
    val mediaItemData: MediaItemData?,
    val audioPlaybackState: AudioPlaybackState
)