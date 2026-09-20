/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.TableOfContents
import com.wapo.flagship.features.articles2.models.TableOfContentsJsonAdapter


open class TableOfContentsTypeConverter {

    @TypeConverter
    fun toJson(tableOfContents: TableOfContents?): String? {
        if(tableOfContents == null)
            return null
        return TableOfContentsJsonAdapter(Moshi.Builder().build()).toJson(tableOfContents)
    }

    @TypeConverter
    fun fromJson(data: String?): TableOfContents?{
        if(data == null)
            return null
        return TableOfContentsJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }

}