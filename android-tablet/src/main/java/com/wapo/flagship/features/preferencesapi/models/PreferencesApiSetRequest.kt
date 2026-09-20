/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.preferencesapi.models

/**
 * Interface that holds the general request body structure for SET requests made to the preferences API.
 * Implement this for any new preference that we use the preferences API for.
 * [value] should be the same type used in [PreferencesApiSetValuesItem] and [PreferencesApiGetValuesItem]
 */
interface PreferencesApiSetRequest {
    val value: Any?
}
