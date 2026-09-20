package com.washingtonpost.android.paywall.features.ftc

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

internal object FTCConsentStorage {
    private const val PREFERENCES_NAME = "ftc_consent"
    private const val CONSENT_TIMESTAMP_KEY = "consent_timestamp"
    private const val CONSENT_TERMS_TEXT_KEY = "consent_terms_text"
    private const val CONSENT_STATUS_KEY = "consent_status"

    suspend fun saveConsent(
        context: Context,
        termsText: String,
        status: ConsentStatus = ConsentStatus.STARTED
    ) = withContext(Dispatchers.IO) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit {
                putLong(CONSENT_TIMESTAMP_KEY, System.currentTimeMillis())
                    .putString(CONSENT_TERMS_TEXT_KEY, termsText)
                putString(CONSENT_STATUS_KEY, status.name)
            }
    }

    suspend fun updateStatus(
        context: Context,
        status: ConsentStatus
    ) = withContext(Dispatchers.IO) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(CONSENT_STATUS_KEY, status.name)
                putLong(CONSENT_TIMESTAMP_KEY, System.currentTimeMillis())
            }
    }

    fun getStoredConsent(context: Context): ConsentData? {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(CONSENT_TIMESTAMP_KEY, -1)
        if (timestamp == -1L) return null

        return ConsentData(
            timestamp = timestamp,
            termsText = prefs.getString(CONSENT_TERMS_TEXT_KEY, ""),
            status = prefs.getString(CONSENT_STATUS_KEY, null)?.let {
                ConsentStatus.valueOf(it)
            } ?: ConsentStatus.STARTED
        )
    }
}

data class ConsentData(
    val timestamp: Long,
    val termsText: String?,
    val status: ConsentStatus
)

enum class ConsentStatus {
    STARTED,
    COMPLETED
}