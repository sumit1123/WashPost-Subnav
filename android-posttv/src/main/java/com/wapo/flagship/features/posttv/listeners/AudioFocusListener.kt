/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv.listeners

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media.AudioAttributesCompat
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager

private val TAG: String = AudioFocusListener::class.java.simpleName

/**
 * Class to handle AudioFocus events for the ExoPlayer maintained by the [PostTvPlayer2Manager] class
 */
class AudioFocusListener(context: Context, val manager: PostTvPlayer2Manager) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioAttributesCompat by lazy {
        AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocusOreo(): Int = audioManager.requestAudioFocus(audioFocusRequest)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocusOreo() = audioManager.abandonAudioFocusRequest(audioFocusRequest)

    @get:RequiresApi(Build.VERSION_CODES.O)
    private val audioFocusRequest by lazy { buildFocusRequest() }

    @TargetApi(Build.VERSION_CODES.O)
    private fun buildFocusRequest(): AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributesCompat.unwrap() as AudioAttributes)
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Logger.d(TAG, "AudioFocusListener: GAIN")
            }
            else -> {
                Logger.d(TAG, "AudioFocusListener: $focusChange")
                manager.pauseMediaOnLostFocus()
            }
        }
    }

    fun requestAudioFocus() {
        var result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = requestAudioFocusOreo()
        } else {
            result = audioManager.requestAudioFocus(
                audioFocusListener,
                // Use the music stream.
                audioAttributesCompat.legacyStreamType,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Logger.d(TAG, "AudioFocusRequest: GRANTED")
        } else {
            Logger.d(TAG, "AudioFocusRequest: $result")
            manager.pauseMediaOnLostFocus()
        }
    }

    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            abandonAudioFocusOreo()
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }
}