package com.wapo.flagship.base

import androidx.lifecycle.LiveData
import com.wapo.flagship.model.Status
import com.wapo.flagship.querypolicies.Query
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
        coroutineContext: CoroutineContext?,
    ): LiveData<Status<out T>>
}

const val REQUEST_TIMEOUT_MS = 30000
