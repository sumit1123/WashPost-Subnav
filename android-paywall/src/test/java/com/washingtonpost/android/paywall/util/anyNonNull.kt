package com.washingtonpost.android.paywall.util

import org.mockito.ArgumentMatchers.any
import kotlin.reflect.KClass

internal inline fun <reified T:Any> anyNonNull(): T {
    return any(T::class.java) ?: createInstance()
}

internal inline fun <reified T : Any> createInstance(): T {
    return when (T::class) {
        Boolean::class -> false as T
        Byte::class -> 0.toByte() as T
        Char::class -> 0.toChar() as T
        Short::class -> 0.toShort() as T
        Int::class -> 0 as T
        Long::class -> 0L as T
        Float::class -> 0f as T
        Double::class -> 0.0 as T
        else -> createInstance(T::class)
    }
}

private fun <T : Any> createInstance(@Suppress("UNUSED_PARAMETER") kClass: KClass<T>): T {
    return castNull()
}

@Suppress("UNCHECKED_CAST")
private fun <T> castNull(): T = null as T