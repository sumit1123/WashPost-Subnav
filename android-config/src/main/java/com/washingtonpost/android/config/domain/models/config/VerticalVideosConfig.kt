package com.washingtonpost.android.config.domain.models.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data Config to hold Vertical Videos config values
 */
@Parcelize
data class VerticalVideosConfig(
    val enabled: Boolean = false,
    val adItemInterval: Int = -1,
    val firstItemDelay: Int = -1,
    val requestsUiTimeoutMillis: Long = -1,
) : Parcelable
