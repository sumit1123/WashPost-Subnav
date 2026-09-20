package com.wapo.flagship.features.audio.playlist.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.audio.playlist.AudioTracker
import com.wapo.flagship.features.audio.playlist.AudioTrackerJsonAdapter

class AudioTrackerTypeConverter {

    val adapter = AudioTrackerJsonAdapter(Moshi.Builder().build())

    @TypeConverter
    fun toJson(audioTracker: AudioTracker?): String? {
        if (audioTracker == null)
            return null
        return adapter.toJson(audioTracker)
    }

    @TypeConverter
    fun fromJson(data: String?): AudioTracker? {
        if (data == null)
            return null
        return adapter.fromJson(data)
    }
}