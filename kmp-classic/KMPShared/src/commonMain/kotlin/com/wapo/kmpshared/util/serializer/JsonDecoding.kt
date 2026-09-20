package com.wapo.kmpshared.util.serializer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Automatically extracts, maps, and validates a nested dictionary.
 * Returns null if the block is missing or if any internal required validation fails.
 */

object JsonEngine {
    val instance =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
}

inline fun <reified T> Map<*, *>.decodeNestedObject(key: String): T? {
    val nestedMap = this[key] as? Map<*, *> ?: return null
    return runCatching {
        JsonEngine.instance.decodeFromJsonElement<T>(nestedMap.toJsonObject())
    }.getOrNull()
}

fun Map<*, *>.toJsonObject(): JsonObject {
    val jsonMap = mutableMapOf<String, JsonElement>()
    for ((key, value) in this) {
        val stringKey = key as? String ?: continue
        jsonMap[stringKey] = value.toJsonElement()
    }
    return JsonObject(jsonMap)
}

fun Any?.toJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is Map<*, *> -> this.toJsonObject()
        is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
        is Boolean -> JsonPrimitive(this)
        // For Android's Moshi support
        is Double -> {
            val asLong = toLong()
            if (isFinite() && this == asLong.toDouble()) {
                JsonPrimitive(asLong)
            } else {
                JsonPrimitive(this)
            }
        }
        // For Android's Moshi support
        is Float -> {
            val asLong = toLong()
            if (isFinite() && this == asLong.toFloat()) {
                JsonPrimitive(asLong)
            } else {
                JsonPrimitive(this)
            }
        }
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        else -> JsonPrimitive(this.toString())
    }
