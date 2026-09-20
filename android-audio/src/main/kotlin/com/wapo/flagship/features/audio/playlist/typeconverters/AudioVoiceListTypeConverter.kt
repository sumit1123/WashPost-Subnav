package com.wapo.flagship.features.audio.playlist.typeconverters

import androidx.room.TypeConverter
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.audio.playlist.AudioVoice
import com.wapo.flagship.features.audio.playlist.AudioVoiceJsonAdapter

class AudioVoiceListTypeConverter {

    val adapter = AudioVoiceJsonAdapter(Moshi.Builder().build())

    @TypeConverter
    fun toJson(items: List<AudioVoice>?): String? {
        if (items == null)
            return null
        val arr = JsonArray()
        items.forEach { arr.add(adapter.toJson(it)) }
        return arr.toString()
    }

    @TypeConverter
    fun fromJson(data: String?): List<AudioVoice>? {
        if (data == null)
            return null
        val items = mutableListOf<AudioVoice>()
        val jsonArray = JsonParser.parseString(data) as JsonArray
        jsonArray.forEach {
            val item = adapter.fromJson(it.asString)
            if (item != null)
                items.add(item)
        }
        return items
    }
}