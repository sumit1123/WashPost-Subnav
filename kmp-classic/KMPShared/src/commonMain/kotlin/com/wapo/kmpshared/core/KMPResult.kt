// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core

sealed class KMPResult<out T> {
    data class Success<out T>(
        val data: T,
    ) : KMPResult<T>()

    data class Error(
        val message: String,
    ) : KMPResult<Nothing>()
}

/**
 * Transforms the data inside a Success result, or propagates the Error as-is.
 */
inline fun <T, R> KMPResult<T>.map(transform: (T) -> R): KMPResult<R> =
    when (this) {
        is KMPResult.Success -> KMPResult.Success(transform(this.data))
        is KMPResult.Error -> KMPResult.Error(this.message)
    }
