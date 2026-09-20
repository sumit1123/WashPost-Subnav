package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.wapo.flagship.features.articles2.models.Renderer
import com.wapo.flagship.features.articles2.models.deserialized.ContextBoxAlignment

/**
 * [TypeConverter] for [Renderer] to be able to serialize and deserialize it
 */
class AlignmentTypeConverter {
    @TypeConverter
    fun toJson(alignment: ContextBoxAlignment?): String? {
        if (alignment == null) {
            return null
        }
        return alignment.value
    }

    @TypeConverter
    fun fromJson(value: String?): ContextBoxAlignment {
        if (value == null) {
            return ContextBoxAlignment.LEFT
        }
        return ContextBoxAlignment.values().find { it.value == value } ?: ContextBoxAlignment.LEFT
    }
}
