package com.wapo.flagship.util.coroutines

import kotlinx.coroutines.CoroutineScope

interface CoroutineScopeProvider {
    val sync: CoroutineScope
}
