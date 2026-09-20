/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.articles2.models.OmnitureXJsonAdapter

/**
 * [TypeConverter] for [OmnitureX] to be able to serialize and deserialize it
 */
open class OmnitureXTypeConverter {

    @TypeConverter
    fun toJson(omnitureX: OmnitureX?): String? {
        if(omnitureX == null)
            return null
        return OmnitureXJsonAdapter(Moshi.Builder().build()).toJson(omnitureX)
    }

    @TypeConverter
    fun fromJson(data: String?): OmnitureX?{
        if(data == null)
            return null
        return OmnitureXJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }

}