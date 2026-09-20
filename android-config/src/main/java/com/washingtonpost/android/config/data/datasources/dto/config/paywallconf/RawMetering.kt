package com.washingtonpost.android.config.data.datasources.dto.config.paywallconf

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywallconf.Metering
import com.washingtonpost.android.config.domain.models.config.paywallconf.WallMap2

/**
 * [mapping] - Maps an action code (ct_tags) received from Tetro to a wall by name.
 */
@JsonClass(generateAdapter = true)
data class RawMetering(
    @Json(name = "sync") val syncUrl: String? = null,
    @Json(name = "queue") val queue: Int? = null,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "mapping") val wallMap: Map<String, String>? = null,
    @Json(name = "mapping2") val wallMap2: List<RawWallMap2>? = null
) {
    fun mapToDomain(): Metering {
        return Metering(
            syncUrl = syncUrl,
            queue = queue,
            age = age,
            wallMap = wallMap,
            wallMap2 = wallMap2?.map { it.mapToDomain() },
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawWallMap2(
    @Json(name = "action") val action: Int? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "blocker") val blocker: String? = null,
    @Json(name = "placement") val placement: Long? = null,
    @Json(name = "version") val version: Int? = null
) {
    fun mapToDomain(): WallMap2 {
        return WallMap2(
            action = action,
            code = code,
            blocker = blocker,
            placement = placement,
            version = version
        )
    }
}