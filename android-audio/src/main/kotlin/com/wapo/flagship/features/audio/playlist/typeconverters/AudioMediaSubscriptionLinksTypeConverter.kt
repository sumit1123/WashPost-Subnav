package com.wapo.flagship.features.audio.playlist.typeconverters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.wapo.flagship.features.audio.config2.AudioMediaSubscriptionLinks

class AudioMediaSubscriptionLinksTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun toJson(links: AudioMediaSubscriptionLinks?): String? = links?.let(gson::toJson)

    @TypeConverter
    fun fromJson(json: String?): AudioMediaSubscriptionLinks? =
        json?.let { gson.fromJson(it, AudioMediaSubscriptionLinks::class.java) }
}
