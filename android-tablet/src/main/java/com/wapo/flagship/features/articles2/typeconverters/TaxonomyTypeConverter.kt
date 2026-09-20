package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.Taxonomy
import com.wapo.flagship.features.articles2.models.TaxonomyJsonAdapter

/**
 * [TypeConverter] for [Taxonomy] to be able to serialize and deserialize it
 */
class TaxonomyTypeConverter {
    @TypeConverter
    fun toJson(taxonomy: Taxonomy?): String? {
        if (taxonomy == null) {
            return null
        }
        return TaxonomyJsonAdapter(Moshi.Builder().build()).toJson(taxonomy)
    }

    @TypeConverter
    fun fromJson(data: String?): Taxonomy? {
        if (data == null) {
            return null
        }
        return TaxonomyJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }
}
