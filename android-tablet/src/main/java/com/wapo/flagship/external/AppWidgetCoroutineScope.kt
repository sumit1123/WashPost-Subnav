package com.wapo.flagship.external

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

object AppWidgetCoroutineScope : CoroutineScope {
    val job = Job()
    override val coroutineContext: CoroutineContext
        get() =
            job + Dispatchers.Main +
                CoroutineName(
                    AppWidgetCoroutineScope::class.java.simpleName,
                )
}
