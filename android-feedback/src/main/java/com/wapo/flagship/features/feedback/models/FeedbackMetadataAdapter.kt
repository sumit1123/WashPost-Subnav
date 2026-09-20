package com.wapo.flagship.features.feedback.models

import com.squareup.moshi.*
import java.io.IOException
import java.lang.reflect.Type

class FeedbackMetadataAdapter(moshi: Moshi) : JsonAdapter<FeedbackMetadata>() {
    private val podcastAdapter = moshi.adapter(PersonalizedPodcastMetadata::class.java)

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: FeedbackMetadata?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        when (value) {
            is PersonalizedPodcastMetadata -> podcastAdapter.toJson(writer, value)
        }
    }

    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): FeedbackMetadata? {
        throw UnsupportedOperationException("Deserialization of FeedbackMetadata requires a type field in the JSON body and is not supported by this adapter.")
    }

    companion object {
        val factory = object : Factory {
            override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
                if (Types.getRawType(type) == FeedbackMetadata::class.java) {
                    return FeedbackMetadataAdapter(moshi)
                }
                return null
            }
        }
    }
}