/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid

import com.google.gson.*
import com.wapo.android.commons.util.Logger
import java.lang.reflect.Type

internal class FusionBaseItemDeserializer: JsonDeserializer<BaseItemEntity?> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): BaseItemEntity? {
            context ?: throw IllegalStateException("Json serialization context is missing")
            return try {
                val jsonObject = (json as? JsonObject)?.get(ITEM_TYPE)
                val type = jsonObject?.let { typeForName(jsonObject.asString) }
                if (type == null) {
                    Logger.w(TAG, "Unknown base item type ${jsonObject?.asString}")
                    return null
                } else {
                    context.deserialize(json, type) as? BaseItemEntity
                }
            } catch (e: Exception) {
                Logger.w(TAG, "Deserialization error", e)
                return null
            }
    }

    companion object {
        @JvmField val ITEM_TYPE = "item_type"
        @JvmField val JsonNames = hashMapOf(
                ItemType.HOMEPAGE_STORY.toString() to HomepageStoryEntity::class.java,
                ItemType.CHAIN.toString() to ChainEntity::class.java,
                ItemType.CAROUSEL_CHAIN.toString() to ChainEntity::class.java,
                ItemType.AD_FLEX_APP.toString() to AdBaseItemEntity::class.java,
                ItemType.AD_BANNER_FLEX_APP.toString() to AdBaseItemEntity::class.java,
                ItemType.SEPARATOR.toString() to SeparatorEntity::class.java,
                ItemType.BREAKING_NEWS_BAR.toString() to BreakingNewsBarEntity::class.java,
                ItemType.LIVE_VIDEO_BAR.toString() to LiveVideoBarEntity::class.java,
                ItemType.CAROUSEL.toString() to CarouselBaseItemEntity::class.java,
                ItemType.HABIT_TILES.toString() to HabitTilesEntity::class.java,
                ItemType.SECTION_TOPPER.toString() to SectionTopperEntity::class.java,
                ItemType.INLINE_OFFER.toString() to InlineOfferEntity::class.java,
        )
        val TAG: String = this.javaClass.simpleName

        private val gson = Gson()

        private fun typeForName(typeString : String) : Type? {
            return JsonNames[typeString]
        }
    }
}