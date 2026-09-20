@file:JvmName("PushUtils")

package com.wapo.android.push

import android.content.Intent
import android.os.Bundle

fun createBundleIntentFromMap(map: Map<String, String>?): Intent {
    return Intent().apply {
        putExtras(Bundle().apply {
            map?.forEach {
                putString(it.key, it.value)
            }
        })
    }
}
