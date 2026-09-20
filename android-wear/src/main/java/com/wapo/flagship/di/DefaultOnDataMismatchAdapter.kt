/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.di

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.io.IOException
import java.lang.reflect.Type

class DefaultOnDataMismatchAdapter<T> private constructor(
    private val delegate: JsonAdapter<T>,
    private val defaultValue: T?
) : JsonAdapter<T?>() {

    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): T? {
        // Use a peeked reader to leave the reader in a known state even if there's an exception.
        val peeked = reader.peekJson()
        var result: T?
        try {
            // Attempt to decode to the target type with the peeked reader.
            result = delegate.fromJson(peeked)
        } catch (e: Exception) {
            result = defaultValue
            try {
                val value = reader.peekJson().readJsonValue().toString()
            } catch (err: Exception) {
            }
        } finally {
            peeked.close()
        }
        // Skip the value back on the reader, no matter the state of the peeked reader.
        reader.skipValue()
        return result
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: T?) {
        delegate.toJson(writer, value)
    }

    companion object {
        fun <T> newFactory(type: Class<T>, defaultValue: T?): Factory {
            return object : Factory {
                override fun create(
                    requestedType: Type, annotations: Set<Annotation?>, moshi: Moshi
                ): JsonAdapter<*>? {
                    if (type != requestedType) return null
                    val delegate = moshi.nextAdapter<T>(this, type, annotations)
                    return DefaultOnDataMismatchAdapter(delegate, defaultValue)
                }
            }
        }
    }

}