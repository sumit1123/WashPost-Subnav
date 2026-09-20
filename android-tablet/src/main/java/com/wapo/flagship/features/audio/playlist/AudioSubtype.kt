package com.wapo.flagship.features.audio.playlist

enum class AudioSubtype(val value: String) {
    PERSONALIZED_PODCAST("personalized_podcast"),
    UNKNOWN("unknown");
    companion object {
        fun fromString(type: String?): AudioSubtype {
            return entries.find { it.value.equals(type, ignoreCase = true) } ?: UNKNOWN
        }
    }
}