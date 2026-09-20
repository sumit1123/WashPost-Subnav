package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ImageConfig

/**
 * Generic data class that holds the information for an image from config.
 * Supports the file name of a local image asset, a url for a remote asset, and a url for remote dark mode asset.
 * The [localFileHasPriority] boolean controls whether to use the local or remote file first, with the other as fallback.
 */
@JsonClass(generateAdapter = true)
data class RawImageConfig(
    @Json(name = "localImageFileName") val localImageFileName: String? = null,
    @Json(name = "remoteImageUrl") val remoteImageUrl: String? = null,
    @Json(name = "remoteImageUrlDarkMode") val remoteImageUrlDarkMode: String? = null,
    @Json(name = "localFileHasPriority") val localFileHasPriority: Boolean? = null,
) {
    fun mapToDomain(): ImageConfig {
        return ImageConfig(
            localImageFileName = localImageFileName,
            remoteImageUrl = remoteImageUrl,
            remoteImageUrlDarkMode = remoteImageUrlDarkMode,
            localFileHasPriority = localFileHasPriority,
        )
    }
}