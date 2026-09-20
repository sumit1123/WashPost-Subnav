// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.util

import java.util.UUID

actual typealias KMPUUID = UUID


actual fun createKMPUUID(): KMPUUID = UUID.randomUUID()
