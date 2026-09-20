package com.wapo.android.commons.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.activity.ComponentActivity

object ViewUtil {

    /**
     * Returns the [Activity] in this [Context] chain that is also of type [T], or null if not found
     *
     * Useful when the provided [Context] is wrapped, such as when using dependency injection
     * frameworks like Hilt, which may return a [ContextWrapper] instead of a direct [Activity]
     * in activities and fragments annotated with @AndroidEntryPoint
     */
    inline fun <reified T> Context.findActivityOfType(): T? {
        //  If context is already an activity return it
        if (this is Activity && this is T) return this

        //  If context is a ContextWrapper, unwrap it and find an activity
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity && context is T) return context
            context = context.baseContext
        }

        //  If not found, return null
        return null
    }

    inline fun <reified T: Activity> Context.requireActivityOfType(): T {
        return findActivityOfType<T>() ?: throw IllegalStateException("Context $this not attached to an activity: ${T::class.java.simpleName}")
    }

    fun View.findActivity(): Activity? = context.findActivity()
    fun View.findComponentActivity(): ComponentActivity? = context.findComponentActivity()
    inline fun <reified T> View.findActivityOfType(): T? = context.findActivityOfType<T>()

    fun Context.findActivity(): Activity? = findActivityOfType<Activity>()
    fun Context.findComponentActivity(): ComponentActivity? = findActivityOfType<ComponentActivity>()
    fun Context.requireActivity(): Activity = requireActivityOfType<Activity>()
    fun Context.requireComponentActivity(): ComponentActivity = requireActivityOfType<ComponentActivity>()
}