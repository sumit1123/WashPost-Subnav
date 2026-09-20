package com.wapo.flagship.features.ask

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media.AudioAttributesCompat
import com.wapo.android.commons.util.Logger

private val TAG: String = TalkToThePostAudioFocusListener::class.java.simpleName

/**
 * Class to handle AudioFocus events for the AudioTrack maintained by the [TalkToThePostAudioManager] class
 */
class TalkToThePostAudioFocusListener(context: Context, private val onAbandonFocus: () -> Unit) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioAttributesCompat by lazy {
        AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .build()
    }

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
                onAbandonFocus.invoke()
            }
        }
    }

    fun requestAudioFocus(): Int {
        val result: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(
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
            onAbandonFocus.invoke()
        }
        return result
    }

    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }
}
