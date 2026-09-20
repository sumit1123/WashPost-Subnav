package com.washingtonpost.android.config.data.datasources.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * This type adapter is used to convert "true"/"false" String to a Boolean
 */
class SafeBooleanAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Boolean? {
        return when (reader.peek()) {
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.STRING -> reader.nextString().lowercase().toBooleanStrictOrNull()
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }

            else -> throw JsonDataException("Expected BOOLEAN or STRING at ${reader.path}")
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Boolean?) {
        writer.value(value)
    }
}