package com.wapo.flagship.features.newsletter.repo.remote.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.wapo.flagship.features.newsletter.domain.models.NewslettersKey

class NewslettersKeyAdapter {
    @ToJson
    fun toJson(key: NewslettersKey): Any {
        return when (key) {
            is NewslettersKey.Id -> key.value
            is NewslettersKey.List -> mapOf("list" to key.value)
        }
    }

    @FromJson
    fun fromJson(value: Any): NewslettersKey {
        return when(value) {
            is String -> NewslettersKey.Id(value)
            is Map<*, *> -> (value["list"] as? String)?.let { NewslettersKey.List(it) }
                ?: throw IllegalArgumentException("Invalid NewslettersKey map: expected key 'list' with String but was ${value["list"]?.let { it::class.java.name }}. Map keys: ${value.keys}")
            else -> throw IllegalArgumentException("Unknown type")
        }
    }
}