// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.util

import java.net.URL

actual typealias KMPURL = URL

actual fun createKMPURLfromString(url: String): KMPURL? =
    try {
        URL(url)
    } catch (e: Exception) {
        null
    }
