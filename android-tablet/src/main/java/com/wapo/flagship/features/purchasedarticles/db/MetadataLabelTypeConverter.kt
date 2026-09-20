package com.wapo.flagship.features.purchasedarticles.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.washingtonpost.android.save.network.MetadataLabel

class MetadataLabelTypeConverter {

    @TypeConverter
    fun fromMetadataLabelObject(value: MetadataLabel): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toMetadataLabelObject(value: String): MetadataLabel {
        val type = object : TypeToken<MetadataLabel>() {}.type
        return Gson().fromJson(value, type)
    }
}