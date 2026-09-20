package com.washingtonpost.android.config.domain.models.config

import android.content.Context

/**
 * Generic data class that holds the information for an image from config.
 * Supports the file name of a local image asset, a url for a remote asset, and a url for remote dark mode asset.
 * The [localFileHasPriority] boolean controls whether to use the local or remote file first, with the other as fallback.
 */
data class ImageConfig(
    val localImageFileName: String?,
    val remoteImageUrl: String?,
    val remoteImageUrlDarkMode: String?,
    val localFileHasPriority: Boolean?,
)

private const val RESOURCE_NOT_FOUND = 0

fun processImageConfig(
    context: Context,
    imageConfig: ImageConfig,
    isNightModeEnabled: Boolean,
): Any? {
    var localImageId =
        context.resources.getIdentifier(
            imageConfig.localImageFileName,
            "drawable",
            context.packageName,
        )
    var remoteImageUrl = imageConfig.remoteImageUrl

    // check for dark mode variants
    if (isNightModeEnabled) {
        val localImageIdDark =
            context.resources.getIdentifier(
                "${imageConfig.localImageFileName}_night",
                "drawable",
                context.packageName,
            )
        if (localImageIdDark != RESOURCE_NOT_FOUND) {
            localImageId = localImageIdDark
        }
        if (!imageConfig.remoteImageUrlDarkMode.isNullOrBlank()) {
            remoteImageUrl = imageConfig.remoteImageUrlDarkMode
        }
    }

    // if localFileHasPriority is true, use local image first and use remote URL as fallback, otherwise inverse
    return when {
        imageConfig.localFileHasPriority == true && localImageId != RESOURCE_NOT_FOUND -> localImageId
        !remoteImageUrl.isNullOrBlank() -> remoteImageUrl
        localImageId != RESOURCE_NOT_FOUND -> localImageId
        else -> null
    }
}