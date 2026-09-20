package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.DisclaimerInfo
import com.wapo.flagship.features.articles2.models.DisclaimerInfoJsonAdapter

class DisclaimerInfoTypeConverter {
    @TypeConverter
    fun toJson(disclaimerInfo: DisclaimerInfo?): String? {
        if (disclaimerInfo == null) {
            return null
        }
        return DisclaimerInfoJsonAdapter(Moshi.Builder().build()).toJson(disclaimerInfo)
    }

    @TypeConverter
    fun fromJson(data: String?): DisclaimerInfo? {
        if (data == null) {
            return null
        }
        return DisclaimerInfoJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }
}