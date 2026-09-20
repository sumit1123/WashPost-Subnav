package com.wapo.flagship.features.grid

import com.google.gson.Gson
import com.google.gson.GsonBuilder

private val PageDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

object FusionMapper {
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(MediaEntity::class.java, MediaEntityDeserializer())
        .registerTypeAdapter(BaseItemEntity::class.java, FusionBaseItemDeserializer())
        .registerTypeAdapter(ItemEntity::class.java, FusionItemDeserializer())
        .setDateFormat(PageDateFormat)
        .create()
}