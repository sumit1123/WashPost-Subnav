package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.Summary
import com.wapo.flagship.features.articles2.models.SummaryJsonAdapter

class SummaryTypeConverter {
    @TypeConverter
    fun toJson(summary: Summary?): String? {
        if (summary == null) {
            return null
        }
        return SummaryJsonAdapter(Moshi.Builder().build()).toJson(summary)
    }

    @TypeConverter
    fun fromJson(data: String?): Summary? {
        if (data == null) {
            return null
        }
        return SummaryJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }
}
