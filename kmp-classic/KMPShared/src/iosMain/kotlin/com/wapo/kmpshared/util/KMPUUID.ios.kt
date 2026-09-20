// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.util

import platform.Foundation.NSUUID

actual typealias KMPUUID = NSUUID

actual fun createKMPUUID(): KMPUUID = NSUUID()
