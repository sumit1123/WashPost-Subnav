package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.deserialized.AutoRecircCarousel
import com.wapo.flagship.features.articles2.models.deserialized.AutoRecircCarouselJsonAdapter

class AutoRecircCarouselTypeConverter {
    @TypeConverter
    fun toJson(data: AutoRecircCarousel?): String? {
        if (data == null) {
            return null
        }
        return AutoRecircCarouselJsonAdapter(Moshi.Builder().build()).toJson(data)
    }

    @TypeConverter
    fun fromJson(data: String?): AutoRecircCarousel? {
        if (data == null) {
            return null
        }
        return AutoRecircCarouselJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }
}