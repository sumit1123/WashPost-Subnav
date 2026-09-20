package com.wapo.flagship.features.articles2.typeconverters

import com.wapo.android.commons.util.Logger
import androidx.room.TypeConverter
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.*
import org.json.JSONObject

/**
 * [TypeConverter] for list of [Item] objects to be able to serialize and deserialize it
 */
open class ElementListTypeConverter {
    @TypeConverter
    fun toJson(items: List<Item>?): String? {
        if (items == null) {
            return null
        }

        val arr = JsonArray()
        items.forEach {
            when (it) {
                is Image -> {
                    arr.add(MoshiAdapters.INSTANCE.imageAdapter.toJson(it))
                }
                is SanitizedHtml -> {
                    arr.add(MoshiAdapters.INSTANCE.sanitizedHtmlAdapter.toJson(it))
                }
                is ListItem -> {
                    arr.add(MoshiAdapters.INSTANCE.listItemAdapter.toJson(it))
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
            try {
                Logger.d("Article_Type_Holder", JSONObject(it.asString)["type"].toString())
                val item =
                    when (JSONObject(it.asString)["type"]) {
                        "image" -> {
                            MoshiAdapters.INSTANCE.imageAdapter.fromJson(it.asString)
                        }
                        "sanitized_html" -> {
                            MoshiAdapters.INSTANCE.sanitizedHtmlAdapter.fromJson(it.asString)
                        }
                        "list" -> {
                            MoshiAdapters.INSTANCE.listItemAdapter.fromJson(it.asString)
                        }
                        else -> {
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("Unexpected data found in feeds item")
                                    setModule(LogModules.ARTICLES)
                                    set("json_element", it)
                                }.run {
                                    RemoteLog.e(FlagshipApplication.getInstance(), build())
                                }
                            Logger.e("ItemListTypeConverter", "Unexpected data found in feeds item: $it")
                            Item("default")
                        }
                    }
                if (item != null) {
                    items.add(item)
                }
            } catch (ex: Exception) {
                // If any exception occurs, we assume that the feeds has a corrupted item in the items list. catch (ex: Exception) {
                items.add(Item("default"))
                EventLog
                    .Builder()
                    .apply {
                        setMessage("An unexpected item found in feeds")
                        setModule(LogModules.ARTICLES)
                        set("json_element", it)
                        setErrorMessage(ex.message)
                    }.run {
                        RemoteLog.e(FlagshipApplication.getInstance(), build())
                    }
                Logger.e("ItemListTypeConverter", "An unexpected item found in feeds: $it")
            }
        }
        return items
    }
}
