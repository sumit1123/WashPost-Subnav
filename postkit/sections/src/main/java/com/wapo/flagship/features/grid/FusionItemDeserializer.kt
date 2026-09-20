package com.wapo.flagship.features.grid

import com.google.gson.*
import com.wapo.android.commons.util.Logger
import java.lang.reflect.Type

internal class FusionItemDeserializer : JsonDeserializer<ItemEntity> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ItemEntity? {
        context ?: throw IllegalStateException("Json serialization context is missing")
        return try {
            if (json is JsonArray) return null
            val jsonObject = (json as? JsonObject)?.get(ITEM_TYPE)
            val type = jsonObject?.let { typeForName(jsonObject.asString) }
            if (type == null) {
                Logger.w(FusionBaseItemDeserializer.TAG, "Unknown item type ${jsonObject?.asString}")
                return null
            } else {
                context.deserialize(json, type) as? ItemEntity
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
                ItemType.AD_FLEX_APP.toString() to AdItemEntity::class.java,
                ItemType.AD_IN_TABLE.toString() to AdItemEntity::class.java,
                ItemType.VOTE.toString() to VoteEntity::class.java,
                ItemType.ELECTIONS_DELAY.toString() to ElectionsDelayEntity::class.java,
                ItemType.CAROUSEL.toString() to CarouselItemEntity::class.java,
                ItemType.PROMO.toString() to HomepageStoryEntity::class.java,
                ItemType.CAROUSEL_VIDEO.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_AUDIO.toString() to CarouselItemEntity::class.java,
                ItemType.IMMERSION_CAROUSEL.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_AUDIO_PLAYLIST_DEPRECATED.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_PERSONALIZED_PODCAST.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_AUDIO_PLAYLIST.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_RECIPE.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_LIVE_IMAGE.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_COMMENTS.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_EXTERNAL.toString() to CarouselItemEntity::class.java,
                ItemType.CAROUSEL_SEVEN_LIVE.toString() to CarouselItemEntity::class.java,
                ItemType.HABIT_TILES.toString() to HabitTilesEntity::class.java,
                ItemType.HABIT_TILES_IN_TABLE.toString() to HabitTilesEntity::class.java,
                ItemType.SECTION_TOPPER.toString() to SectionTopperEntity::class.java,
                ItemType.INLINE_OFFER.toString() to InlineOfferEntity::class.java
        )
        val TAG: String = this.javaClass.simpleName

        private val gson = Gson()

        private fun typeForName(typeString : String) : Type? {
            return JsonNames[typeString]
        }
    }
}