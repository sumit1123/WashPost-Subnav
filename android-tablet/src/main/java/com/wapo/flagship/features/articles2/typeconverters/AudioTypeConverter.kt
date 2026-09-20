// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.articles2.models.deserialized.AudioJsonAdapter

open class AudioTypeConverter {
    @TypeConverter
    fun toJson(audio: Audio?): String? {
        if (audio == null) {
            return null
        }
        return AudioJsonAdapter(Moshi.Builder().build()).toJson(audio)
    }

    @TypeConverter
    fun fromJson(data: String?): Audio? {
        if (data == null) {
            return null
        }
        return AudioJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }
}
