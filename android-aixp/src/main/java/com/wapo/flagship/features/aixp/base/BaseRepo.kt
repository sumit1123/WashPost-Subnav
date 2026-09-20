/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.base

import androidx.lifecycle.LiveData
import com.wapo.flagship.features.aixp.models.Status
import com.wapo.flagship.features.aixp.querypolicies.Query
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Base class for every single Repository in the app.
 * Takes care of the background work, exceptions, and separate job exceptions.
 */
interface BaseRepo<T> {

    fun fetchData(
        query: Query<T>,
        viewModelScope: CoroutineScope?,
        coroutineContext: CoroutineContext?
    ): LiveData<Status<out T>>
}

const val AI_XP_REQUEST_TIMEOUT_MS = 2500