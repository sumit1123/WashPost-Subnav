package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.Targeting
import com.wapo.flagship.features.articles2.models.TargetingJsonAdapter

class TargetingTypeConverter {
    @TypeConverter
    fun toJson(targeting: Targeting?): String? {
        if (targeting == null) {
            return null
        }
        return TargetingJsonAdapter(Moshi.Builder().build()).toJson(targeting)
    }

    @TypeConverter
    fun fromJson(data: String?): Targeting? {
        if (data == null) {
            return null
        }
        return TargetingJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }
}
