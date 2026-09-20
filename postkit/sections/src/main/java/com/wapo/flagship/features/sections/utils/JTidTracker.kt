package com.wapo.flagship.features.sections.utils

object JTidTracker {
    private var _jTid: Long = -1L

    @JvmStatic
    val currentJTid
        get() = _jTid

    @JvmStatic
    val updatedJTid: Long
        get() {
            _jTid = System.currentTimeMillis()
            return _jTid
        }

}