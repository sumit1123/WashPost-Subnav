package com.wapo.flagship.features.gifting.models

/**
 * A status class to tell the status of the remaining count api to the view model(s)
 */
sealed class RemainingCountApiStatus {
    /**
     * The api has been successful with at least 1 remaining article that user can gift.
     * [hasAlreadyShared] -  TRUE If user already gifted the same article in the past.
     */
    data class Success(
        val remainingCount: Int,
        val hasAlreadyShared: Boolean,
    ) : RemainingCountApiStatus()

    /**
     * Any type of general failure including n/w failure.
     */
    object Failure : RemainingCountApiStatus()

    /**
     * No remaining articles left for user to gift.
     */
    object NoRemainingArticles : RemainingCountApiStatus()
}
