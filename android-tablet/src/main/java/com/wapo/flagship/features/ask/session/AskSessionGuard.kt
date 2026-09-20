package com.wapo.flagship.features.ask.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AskSessionOwner {
    ANDROID_AUTO,
    PHONE,
}

/**
 * Prevents the phone and Android Auto Ask experiences from controlling the shared SSE session at
 * the same time.
 */
object AskSessionGuard {
    private val _owner = MutableStateFlow<AskSessionOwner?>(null)
    val owner: StateFlow<AskSessionOwner?> = _owner.asStateFlow()

    @Synchronized
    fun tryAcquire(requestedOwner: AskSessionOwner): Boolean {
        if (_owner.value != null) return false
        _owner.value = requestedOwner
        return true
    }

    @Synchronized
    fun release(releasingOwner: AskSessionOwner) {
        if (_owner.value == releasingOwner) {
            _owner.value = null
        }
    }

    fun isOwnedBy(expectedOwner: AskSessionOwner): Boolean = owner.value == expectedOwner
}
