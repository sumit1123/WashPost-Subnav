package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.FtsCarousel
import com.wapo.flagship.features.articles2.models.FtsCarouselJsonAdapter

class FtsCarouselTypeConverter {
    @TypeConverter
    fun toJson(ftsFooter: FtsCarousel?): String? {
        if (ftsFooter == null) {
            return null
        }
        return FtsCarouselJsonAdapter(Moshi.Builder().build()).toJson(ftsFooter)
    }

    @TypeConverter
    fun fromJson(data: String?): FtsCarousel? {
        if (data == null) {
            return null
        }
        return FtsCarouselJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }
}
