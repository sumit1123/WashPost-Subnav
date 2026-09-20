package com.wapo.flagship.features.preferencesapi

import com.wapo.flagship.features.preferencesapi.models.NewsprintAttributesValueItem
import com.wapo.flagship.features.preferencesapi.models.NewsprintStateValueItem

/**
 * A status class to tell the status of the user subscribed newsprint attributes to the view model(s)
 */
sealed class GetUserNewsprintAttributesApiStatus {

    /**
     * The api has been successful and returns the [preference]
     * that have been subscribed to
     */
    data class Success(
        val preference: NewsprintAttributesValueItem?
    ) : GetUserNewsprintAttributesApiStatus()

    /**
     * Any type of general failure including n/w failure.
     */
    data class Failure(val message: String) : GetUserNewsprintAttributesApiStatus()

    /**
     * This user has seen the newsprint screen and selected none of them.
     */
    object NoData : GetUserNewsprintAttributesApiStatus()

    /**
     * This user has never seen the newsprint attributes before.
     */
    data object NewUser : GetUserNewsprintAttributesApiStatus()
}

/**
 * A status class to tell the status of the user subscribed newsprint state to the view model(s)
 */
sealed class GetUserNewsprintStateApiStatus {

    /**
     * The api has been successful and returns the [preference]
     * that have been subscribed to
     */
    data class Success(
        val preference: NewsprintStateValueItem?
    ) : GetUserNewsprintStateApiStatus()

    /**
     * Any type of general failure including n/w failure.
     */
    data class Failure(val message: String) : GetUserNewsprintStateApiStatus()

    /**
     * This user has seen the newsprint screen and selected none of them.
     */
    object NoData : GetUserNewsprintStateApiStatus()

    /**
     * This user has never seen the newsprint state before.
     */
    data object NewUser : GetUserNewsprintStateApiStatus()
}
