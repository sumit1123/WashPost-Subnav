package com.wapo.flagship.util

/**
 * Tracks the most recent push notification id that opened / entered the current app session.
 *
 * The setter deliberately ignores null / blank values so that a subsequent non-push article
 * open (which may still carry a tracking helper whose pushId is null) does not wipe out the
 * id captured from the originating push notification. Callers that want to reset the value
 * (e.g. on session end) must invoke [clear] explicitly.
 */
object PushIdTracker {

    @Volatile
    private var _pushId: String? = null

    var pushId: String?
        get() = _pushId
        set(value) {
            if (!value.isNullOrBlank()) {
                _pushId = value
            }
        }

    fun clear() {
        _pushId = null
    }
}
