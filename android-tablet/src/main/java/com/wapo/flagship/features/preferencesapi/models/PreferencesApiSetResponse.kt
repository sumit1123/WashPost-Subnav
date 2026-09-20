/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.preferencesapi.models

/**
 * Interface that holds the general structure for responses from SET calls made to the preferences API.
 * Implement this for any new preference that we use the preferences API for.
 */
interface PreferencesApiSetResponse {
    val preferenceValue: PreferencesApiSetValuesItem?
    val state: String?
    val status: String?
}

/**
 * Interface that holds the general structure for the "preferenceValue" field in responses from SET calls made to the preferences API.
 * Implement this for any new preference that we use the preferences API for.
 * Make sure to use the correct data structure for [value] (implement one if needed).
 */
interface PreferencesApiSetValuesItem {
    val lastUpdated: Long?
    val dateCreated: Long?
    val id: Id?
    val value: Any?
}
