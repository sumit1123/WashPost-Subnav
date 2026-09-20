/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid

import com.google.gson.*
import java.lang.reflect.Type

internal class MediaEntityDeserializer : JsonDeserializer<MediaEntity> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MediaEntity? {
        context ?: throw IllegalStateException("Json serialization context is missing")
        val jObj = json as? JsonObject ?: return null

        return MediaEntity(
                url = if (jObj.get(URL) is JsonNull) null else jObj.getAsJsonPrimitive(URL)?.asString,
                mediaType = gson.fromJson(json.get(MEDIA_TYPE), MediaTypeEntity::class.java),
                width = jObj.getInt(WIDTH, 0),
                height = jObj.getInt(HEIGHT, 0),
                aspectRatio = jObj.getFloat(ASPECT_RATIO, 1.5f),
                overlay = gson.fromJson(json.get(OVERLAY), OverlayEntity::class.java),
                caption = jObj.getString(CAPTION, ""),
                promoImageURL = if (jObj.get(PROMO_IMAGE_URL) is JsonNull) null else jObj.getAsJsonPrimitive(PROMO_IMAGE_URL)?.asString,
                video = gson.fromJson(json.get(VIDEO), VideoEntity::class.java),
                liveImage = if (jObj.get(LIVE_IMAGE) is JsonNull) null else gson.fromJson(json.getAsJsonObject(LIVE_IMAGE), LiveImageEntity::class.java),
                link = gson.fromJson(json.get(LINK), LinkEntity::class.java),
                dynamicReplacement =  if (jObj.get(DYNAMIC_REPLACEMENT) is JsonNull) null else gson.fromJson(json.get(DYNAMIC_REPLACEMENT), SubItemTypeEntity::class.java),
                altText = if (jObj.get(ALT_TEXT) is JsonNull) null else jObj.getAsJsonPrimitive(ALT_TEXT)?.asString,
                bleed = if (jObj.get(BLEED) is JsonNull) BleedEntity.NONE else gson.fromJson(json.get(BLEED), BleedEntity::class.java),
                makeItRound = if (jObj.get(MAKE_IT_ROUND) is JsonNull) false else jObj.getAsJsonPrimitive(MAKE_IT_ROUND)?.asBoolean
        )
    }

    private fun JsonObject.getFloat(prop: String, default: Float): Float {
        return try {
            this.getAsJsonPrimitive(prop)?.asFloat ?: default
        } catch(e: Exception) {
            default
        }
    }

    private fun JsonObject.getInt(prop: String, default: Int): Int {
        return try {
            this.getAsJsonPrimitive(prop)?.asInt ?: default
        } catch(e: Exception) {
            default
        }
    }

    private fun JsonObject.getString(prop: String, default: String): String{
        return try {
            this.getAsJsonPrimitive(prop)?.asString ?: default
        } catch(e: Exception) {
            default
        }
    }

    companion object {
        @JvmField val ASPECT_RATIO = "aspect_ratio"
        @JvmField val MEDIA_TYPE = "media_type"
        @JvmField val OVERLAY = "overlay"
        @JvmField val WIDTH = "width"
        @JvmField val HEIGHT = "height"
        @JvmField val CAPTION = "caption"
        @JvmField val URL = "url"
        @JvmField val PROMO_IMAGE_URL = "promo_image"
        @JvmField val VIDEO = "video"
        @JvmField val LIVE_IMAGE = "live_image"
        @JvmField val LINK = "link"
        @JvmField val DYNAMIC_REPLACEMENT = "dynamic_replacement"
        @JvmField val ALT_TEXT = "alt_text"
        @JvmField val BLEED = "bleed"
        @JvmField val MAKE_IT_ROUND = "make_it_round"

        private val gson = Gson()
    }
}