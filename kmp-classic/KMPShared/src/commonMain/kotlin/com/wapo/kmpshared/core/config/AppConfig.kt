// Copyright (c) 2026 The Washington Post. All rights reserved.
@file:UseSerializers(KMPURLSerializer::class)

package com.wapo.kmpshared.core.config

import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.util.KMPURL
import com.wapo.kmpshared.util.serializer.KMPURLSerializer
import com.wapo.kmpshared.util.serializer.decodeNestedObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Root configuration object for the KMP shared library, parsed from a single
 * remote-config JSON payload shared across iOS and Android.
 *
 * iOS and Android both pass the raw, untyped JSON (as a [Map]) into [AppConfig]'s
 * secondary constructor; KMP owns all parsing and validation via `kotlinx.serialization`.
 * This keeps a single source of truth for config shape and avoids duplicating
 * parsing logic in Swift and Kotlin.
 *
 * Each nested config (e.g. [ConversationsConfig], [FeedbackServiceConfig]) is
 * optional: if its block is missing from the JSON, or fails to decode (e.g. a
 * required field is missing or malformed), that feature's config will be `null`
 * rather than causing the whole [AppConfig] to fail to parse.
 *
 * Example minimal JSON:
 * ```json
 * {
 *   "conversations": {
 *     "url": "https://example.com/conversations",
 *     "displayNameURLv2": "https://example.com/display-name",
 *     "conversationSummaryURL": "https://example.com/summary",
 *     "communityGuidelinesURL": "https://example.com/guidelines",
 *     "conversationSettingsURL": "https://example.com/settings",
 *     "topLevelPageSize": 20,
 *     "replyPageSize": 10,
 *     "displayNameMinCharacters": 3
 *   }
 * }
 * ```
 * In this example, `feedback` would be `null` since `userFeedback` is absent.
 */

data class AppConfig(
    val version: Int,
    val logger: LoggerConfig?,
    val feedback: FeedbackServiceConfig?,
    val conversations: ConversationsConfig?,
) {
    constructor(
        version: Int,
        json: Map<String, Any?>,
    ) : this(
        version = version,
        logger = json.decodeNestedObject("logger"),
        feedback = json.decodeNestedObject("userFeedback"),
        conversations = json.decodeNestedObject("conversations"),
    )
}

@Serializable
data class LoggerConfig(
    val uploaderURL: KMPURL,
    val uploaderToken: String,
    val maxLogAge: Double = 8.0 * 60 * 60,
    val maxLogSize: Long = 16_384L,
    val maxLogFiles: Int = 64,
    val sampling: Map<String, Float> = emptyMap(),
    val minLogLevel: LogLevel? = null,
)

@Serializable
data class FeedbackServiceConfig(
    @SerialName("url") val baseURL: KMPURL,
)

@Serializable
data class ConversationsConfig(
    @SerialName("url") val baseURL: KMPURL,
    @SerialName("displayNameURLv2") val displayNameURL: KMPURL,
    val conversationSummaryURL: KMPURL,
    val communityGuidelinesURL: KMPURL,
    val conversationSettingsURL: KMPURL,
    val topLevelPageSize: Int = 12,
    val replyPageSize: Int = 5,
    val displayNameMinCharacters: Int = 6,
)
