package com.wapo.flagship.features.preferencesapi

import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem

/**
 * A status class to tell the status of the available content packs to the view model(s)
 */
sealed class ContentPacksListApiStatus {
    /**
     * The api has been successful and returns the [contentPacks]
     * that have been subscribed to
     */
    data class Success(
        val contentPacks: List<ContentPackUiItem?>,
    ) : ContentPacksListApiStatus()

    /**
     * Any type of general failure including n/w failure.
     */
    data class Failure(
        val message: String,
    ) : ContentPacksListApiStatus()
}
