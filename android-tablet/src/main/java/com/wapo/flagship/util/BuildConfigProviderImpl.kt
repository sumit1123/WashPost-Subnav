/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.wapo.flagship.util

import com.wapo.android.commons.domain.BuildConfigProvider
import com.washingtonpost.android.BuildConfig

class BuildConfigProviderImpl: BuildConfigProvider {

    override fun getStoreType(): String {
        return BuildConfig.STORE_TYPE
    }
}
