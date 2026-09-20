/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.Editorpick
import com.wapo.flagship.features.articles2.models.EditorpickJsonAdapter
import com.wapo.flagship.features.articles2.models.Item

/**
 * [TypeConverter] for list of [Editorpick] objects to be able to serialize and deserialize it
 */
class EditorPickListTypeConverter {

    @TypeConverter
    fun toJson(items: List<Editorpick>?): String? {
        if(items == null)
            return null

        val arr = JsonArray()
        items.forEach {
            arr.add(EditorpickJsonAdapter(Moshi.Builder().build()).toJson(it))
        }
        return arr.toString()
    }

    @TypeConverter
    fun fromJson(data: String?): List<Editorpick>?{
        if(data == null)
            return null
        val items = mutableListOf<Editorpick>()
        val jsonArray = JsonParser.parseString(data) as JsonArray
        jsonArray.forEach{
            val item = EditorpickJsonAdapter(Moshi.Builder().build()).fromJson(it.asString)
            if(item != null)
                items.add(item)
        }

        return items
    }

}