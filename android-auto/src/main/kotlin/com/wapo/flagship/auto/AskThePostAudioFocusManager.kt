package com.wapo.flagship.auto

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
internal class AskThePostAudioFocusManager(
    context: Context,
    private val onFocusChanged: (AskThePostAudioFocusChange) -> Unit,
) {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    hasActiveFocusRequest = true
                    hasFocus = true
                    onFocusChanged(AskThePostAudioFocusChange.GAINED)
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    hasActiveFocusRequest = false
                    hasFocus = false
                    onFocusChanged(AskThePostAudioFocusChange.LOST)
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                    -> {
                    hasFocus = false
                    onFocusChanged(AskThePostAudioFocusChange.LOST_TRANSIENTLY)
                }
            }
        }

    private val focusRequest by lazy {
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            ).setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    }

    var hasFocus = false
        private set
    private var hasActiveFocusRequest = false

    fun requestFocus(): Boolean {
        if (hasActiveFocusRequest) return true
        val result =
            audioManager.requestAudioFocus(focusRequest)
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasActiveFocusRequest = hasFocus
        return hasActiveFocusRequest
    }

    fun abandonFocus() {
        if (!hasActiveFocusRequest) return
        hasActiveFocusRequest = false
        hasFocus = false
        audioManager.abandonAudioFocusRequest(focusRequest)
    }
}

internal enum class AskThePostAudioFocusChange {
    GAINED,
    LOST_TRANSIENTLY,
    LOST,
}
