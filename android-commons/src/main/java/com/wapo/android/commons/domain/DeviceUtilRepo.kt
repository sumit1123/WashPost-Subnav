/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.domain

import java.io.File

interface DeviceUtilRepo {
    fun isTablet(): Boolean
    fun getDataDirectory(): File
    fun getAppDirectory(): File?
    fun getUniqueDeviceId(): String?
    fun getDeviceSerialId(): String?
    fun getDeviceName(): String
    fun generateJUcid(): String
    fun getLoggingId(): String?
    fun getNewLoggingId(): String?
    fun getNumberOfCores(): Int

    companion object {
        const val TABLET_MIN_WIDTH = 600
        const val TABLET_LANDSCAPE_MIN_WIDTH = 1024
    }
}
