package com.wapo.flagship.features.personalizedpodcasts.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TranscriptResponse(
    @Json(name = "metadata") val metadata: TranscriptMetadata,
    @Json(name = "dialogue") val dialogue: List<DialogueSegment>
)

@JsonClass(generateAdapter = true)
data class TranscriptMetadata(
    @Json(name = "total_segments") val totalSegments: Int,
    @Json(name = "total_dialogue_entries") val totalDialogueEntries: Int,
    @Json(name = "total_duration") val totalDuration: Double,
    @Json(name = "has_timestamps") val hasTimestamps: Boolean
)

@JsonClass(generateAdapter = true)
data class DialogueSegment(
    @Json(name = "speaker") val speaker: String,
    @Json(name = "text") val text: String,
    @Json(name = "start_time") val startTime: Double,
    @Json(name = "end_time") val endTime: Double,
    @Json(name = "duration") val duration: Double,
    @Json(name = "char_count") val charCount: Int
) {
    fun isActive(currentPositionMs: Long): Boolean {
        val startMs = startTime.toLong()
        val endMs = endTime.toLong()
        return currentPositionMs in startMs..endMs
    }
}

