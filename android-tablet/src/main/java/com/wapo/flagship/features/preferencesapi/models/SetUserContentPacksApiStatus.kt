package com.wapo.flagship.features.preferencesapi.models

/**
 * A status class to tell the status of the user subscribed content packs to the view model(s)
 */
sealed class SetUserContentPacksApiStatus {
    /**
     * The api has been successful and returns the [contentPacks]
     * that have been subscribed to
     */
    data class Success(
        val contentPacks: List<ContentPacksValueItem?>,
    ) : SetUserContentPacksApiStatus()

    /**
     * Any type of general failure including n/w failure.
     */
    data class Failure(
        val message: String,
    ) : SetUserContentPacksApiStatus()
}
