/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio.utils

import android.content.Context
import com.wapo.flagship.features.audio.R

class AudioViewUtils {
    companion object {
        /**
         * Creates string value representing the audio item's duration
         * @param durationInSeconds the preferred voice audio's duration, in seconds
         * @return a string representing the preferred voice audio's duration
         */
        fun getDurationText(durationInSeconds: Long?, context: Context): String {
            durationInSeconds ?: return context.resources.getString(R.string.listen)

            val playbackSpeed = AudioPreferences.getAudioPlaybackSpeed(context)
            val adjustedDuration = (durationInSeconds / playbackSpeed).toInt()

            val hours = adjustedDuration / 3600
            var minutes = (adjustedDuration % 3600) / 60
            val seconds = adjustedDuration % 60
            if (minutes > 0 && seconds >= 30) { minutes += 1 }

            return when {
                (hours > 0) -> "$hours hr $minutes min"
                (minutes > 0) -> "$minutes min"
                else -> "$seconds sec"
            }
        }

        fun getDurationForPlaylist(durationInSeconds: Long?, context: Context): String {
            durationInSeconds ?: return ""
            val playbackSpeed = AudioPreferences.getAudioPlaybackSpeed(context)
            val adjustedDuration = (durationInSeconds / playbackSpeed).toInt()
            val duration: Int
            val totalSeconds = adjustedDuration
            if (totalSeconds <= 0) {
                duration = durationInSeconds.toInt()
            } else {
                duration = totalSeconds
            }
            val hours = duration / 3600
            var minutes = (duration % 3600) / 60
            val seconds = duration % 60
            if (minutes > 0 && seconds >= 30) {
                minutes += 1
            }

            return when {
                (hours > 0) -> "$hours hr $minutes min"
                (minutes > 0) -> "$minutes min"
                else -> "$seconds sec"
            }
        }
    }
}