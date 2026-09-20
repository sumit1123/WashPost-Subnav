// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.util

expect class KMPURL

expect fun createKMPURLfromString(url: String): KMPURL?
