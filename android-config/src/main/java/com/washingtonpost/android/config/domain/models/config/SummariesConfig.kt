package com.washingtonpost.android.config.domain.models.config

data class SummariesConfig(
    val enabled: Boolean,
    val realtimeSummary: RealtimeSummary,
    val realtimeFeedback: RealtimeFeedback
)

data class RealtimeSummary(
    val enabled: Boolean,
    val url: String,
)

data class RealtimeFeedback(
    val enabled: Boolean,
    val url: String,
)
