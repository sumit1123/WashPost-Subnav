package com.wapo.flagship.features.audio.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.wapo.flagship.features.audio.models.PlaybackSpeed
import com.wapo.flagship.features.audio.models.PlaybackVoice

/**
 * This object stores all of the preferences in the audio module.
 */
object AudioPreferences {

    private const val PREF_AUDIO_PLAYBACK_SPEED = "PREF_POLLY_PLAYBACK_SPEED"

    fun getPreferredPlaybackVoice(voiceList: List<PlaybackVoice>?): PlaybackVoice? {
        return voiceList?.firstOrNull()
    }

    fun setAudioPlaybackSpeed(speed: Float, context: Context) {
        val sharedPrefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        sharedPrefs.edit().putFloat(PREF_AUDIO_PLAYBACK_SPEED, speed).apply()
    }

    fun getAudioPlaybackSpeed(context: Context): Float {
        val sharedPrefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        return sharedPrefs.getFloat(PREF_AUDIO_PLAYBACK_SPEED, PlaybackSpeed.Normal().speed)
    }
}