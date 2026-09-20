// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.util

import platform.Foundation.NSURL

actual typealias KMPURL = NSURL

actual fun createKMPURLfromString(url: String): KMPURL? = NSURL(string = url)
