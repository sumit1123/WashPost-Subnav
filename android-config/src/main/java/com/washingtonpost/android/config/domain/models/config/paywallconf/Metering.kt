package com.washingtonpost.android.config.domain.models.config.paywallconf

/**
 * [mapping] - Maps an action code (ct_tags) received from Tetro to a wall by name.
 */
data class Metering(
    val syncUrl: String?,
    val queue: Int?,
    val age: Int?,
    val wallMap: Map<String, String>?,
    val wallMap2: List<WallMap2>?
)

data class WallMap2(
    val action: Int?,
    val code: String?,
    val blocker: String?,
    val placement: Long?,
    val version: Int?
)