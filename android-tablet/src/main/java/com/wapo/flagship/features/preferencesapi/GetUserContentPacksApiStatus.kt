package com.wapo.flagship.features.preferencesapi

import com.wapo.flagship.features.preferencesapi.models.ContentPacksValueItem

/**
 * A status class to tell the status of the user subscribed content packs to the view model(s)
 */
sealed class GetUserContentPacksApiStatus {
    /**
     * The api has been successful and returns the [contentPacks]
     * that have been subscribed to
     */
    data class Success(
        val contentPacks: List<ContentPacksValueItem?>,
    ) : GetUserContentPacksApiStatus()

    /**
     * Any type of general failure including n/w failure.
     */
    data class Failure(
        val message: String,
    ) : GetUserContentPacksApiStatus()

    /**
     * This user has seen the content packs screen and selected none of them.
     */
    object NoUserContentPacks : GetUserContentPacksApiStatus()

    /**
     * This user has never seen the content packs before.
     */
    object NewUser : GetUserContentPacksApiStatus()
}
