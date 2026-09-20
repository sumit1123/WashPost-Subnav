/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.data.repository

import android.os.Build
import com.wapo.android.commons.domain.BuildProviderRepo
import javax.inject.Inject

class BuildProviderRepoImpl @Inject constructor() : BuildProviderRepo {

    override fun getManufacturer(): String {
        return Build.MANUFACTURER
    }

    override fun getModel(): String {
        return Build.MODEL
    }

    override fun getVersionSdkInt(): Int {
        return Build.VERSION.SDK_INT
    }

    override fun getVersionCodesLollipopMR1(): Int {
        return Build.VERSION_CODES.LOLLIPOP_MR1
    }

    override fun getUnknown(): String {
        return Build.UNKNOWN
    }
}
