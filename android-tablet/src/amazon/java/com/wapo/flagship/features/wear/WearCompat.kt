package com.wapo.flagship.features.wear

import android.content.Context

/**
 * Created by kattim on 2/28/18.
 */
class WearCompat {
    fun canSupportWear(): Boolean = false

    fun onCreate(context: Context) {
        // not supported
    }

    fun onResume() {
        // not supported
    }

    fun onPause() {
        // not supported
    }
}
