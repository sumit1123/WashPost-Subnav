package com.wapo.flagship.features.extentions

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Returns true if the device configuration is tablet, false otherwise.
 */
fun Context.isTablet(): Boolean {
    if (resources != null) {
        val screenSize = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenSize > Configuration.SCREENLAYOUT_SIZE_NORMAL
    }
    return false //Normally we wont reach here
}

inline fun View.launchWhenAttached(
    crossinline block: suspend CoroutineScope.() -> Unit
): Job? {
    val lifecycleOwner = findViewTreeLifecycleOwner()

    // If already attached, launch immediately
    if (lifecycleOwner != null && isAttachedToWindow) {
        return lifecycleOwner.lifecycleScope.launch { block() }
    }

    // Otherwise, wait for the attachment event
    var job: Job? = null
    val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            val owner = findViewTreeLifecycleOwner()
            job = owner?.lifecycleScope?.launch { block() }
        }
        override fun onViewDetachedFromWindow(v: View) {
            job?.cancel()
            v.removeOnAttachStateChangeListener(this)
        }
    }
    addOnAttachStateChangeListener(listener)
    return job
}