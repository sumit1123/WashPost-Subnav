package com.wapo.flagship.features.amazonunification.models

import androidx.room.TypeConverter
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.ByLine
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.articles2.typeconverters.MoshiAdapters
import org.json.JSONObject

/**
 * Converter class for items member in [RainbowArticle] class
 */
class RainbowItemListTypeConverter {
    @TypeConverter
    fun toJson(items: List<Item>?): String? {
        val arr = JsonArray()
        items?.forEach {
            when (it) {
                is ByLine -> {
                    arr.add(MoshiAdapters.INSTANCE.bylineAdapter.toJson(it))
                }
                is Date -> {
                    arr.add(MoshiAdapters.INSTANCE.dateAdapter.toJson(it))
                }
            }
        }
        return arr.toString()
    }

    @TypeConverter
    fun fromJson(data: String?): List<Item>? {
        if (data == null) {
            return null
        }
        val items = mutableListOf<Item>()
        val jsonArray = JsonParser.parseString(data) as JsonArray
        jsonArray.forEach {
            val item =
                when (JSONObject(it.asString)["type"]) {
                    "byline" -> {
                        MoshiAdapters.INSTANCE.bylineAdapter.fromJson(it.asString)
                    }
                    "date" -> {
                        MoshiAdapters.INSTANCE.dateAdapter.fromJson(it.asString)
                    }
                    else -> null
                }
            if (item != null) {
                items.add(item)
            }
        }
        return items
    }
}
