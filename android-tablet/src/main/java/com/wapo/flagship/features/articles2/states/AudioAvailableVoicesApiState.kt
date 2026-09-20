package com.wapo.flagship.features.articles2.states

import com.wapo.flagship.features.articles2.models.Voices

/**
 * This is the state of the api call for downloading the available voices from the manifest url that is given in an audio item from feeds.
 */
sealed class AudioAvailableVoicesApiState {
    /**
     * Request made and has not returned.
     */
    object Loading : AudioAvailableVoicesApiState()

    /**
     * Request failed (we can add properties and make this object a class if needed to show error on the UI).
     */
    object Failure : AudioAvailableVoicesApiState()

    /**
     * Request succeeded
     * [voices] response of the request.
     */
    class AvailableVoices(
        val voices: Voices,
    ) : AudioAvailableVoicesApiState()
}
