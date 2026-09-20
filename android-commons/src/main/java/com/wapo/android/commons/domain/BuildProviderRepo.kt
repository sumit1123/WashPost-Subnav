/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.domain

/**
 * A repository to provide access to static Build properties
 */
interface BuildProviderRepo {
    fun getManufacturer(): String

    fun getModel(): String

    fun getVersionSdkInt(): Int

    fun getVersionCodesLollipopMR1(): Int

    fun getUnknown(): String
}
