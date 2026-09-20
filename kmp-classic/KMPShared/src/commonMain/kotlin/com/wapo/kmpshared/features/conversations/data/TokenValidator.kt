package com.wapo.kmpshared.features.conversations.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds

@Serializable
private data class JWTPayload(
    val exp: Long? = null,
)

@Single
class TokenValidator {
    private val coder = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true

        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true

            // Add padding if missing (some decoders require this)
            val payloadBase64 =
                parts[1].let {
                    when (it.length % 4) {
                        2 -> "$it=="
                        3 -> "$it="
                        else -> it
                    }
                }

            val decodedBytes = Base64.UrlSafe.decode(payloadBase64)
            val jwtPayload = coder.decodeFromString<JWTPayload>(decodedBytes.decodeToString())

            val expiry = jwtPayload.exp?.let { Instant.fromEpochSeconds(it) } ?: return true

            // Check if current time is past the expiry minus buffer
            val now = Clock.System.now()
            val buffer = 60.seconds // Using Duration API if available

            now >= (expiry - buffer)
        } catch (e: Exception) {
            true
        }
    }
}
