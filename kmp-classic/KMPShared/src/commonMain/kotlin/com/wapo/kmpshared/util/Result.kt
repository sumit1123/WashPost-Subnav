package com.wapo.kmpshared.util

// Kotlin Result does not implement flatMap :(
internal inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(onSuccess = { transform(it) }, onFailure = { Result.failure(it) })
