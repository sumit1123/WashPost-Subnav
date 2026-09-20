/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils.coroutines

import kotlinx.coroutines.CoroutineScope

interface CoroutineScopeProvider {
    val sync: CoroutineScope
}