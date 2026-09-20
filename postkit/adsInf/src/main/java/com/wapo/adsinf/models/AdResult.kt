package com.wapo.adsinf.models

sealed class AdResult<out T> {
    data class Success<out T>(val value: T) : AdResult<T>()
    data class Failure(val error: AdError) : AdResult<Nothing>()
}