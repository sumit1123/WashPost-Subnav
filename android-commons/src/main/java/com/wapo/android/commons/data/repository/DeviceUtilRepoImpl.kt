/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.DisplayMetrics
import com.wapo.android.commons.domain.BuildProviderRepo
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import androidx.core.content.edit

class DeviceUtilRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val buildProviderRepo: BuildProviderRepo
) : DeviceUtilRepo {

    private val TAG = DeviceUtilRepoImpl::class.java.simpleName
    private val PREF_NEW_LOGGING_ID = "pref.new.logging_id"
    private val PREF_LOGGING_ID = "pref.logging_id"
    private val PREF_DEVICE_SERIAL_ID = "pref.serial_id"

    override fun isTablet(): Boolean {
        val res: Resources = context.resources
        val config: Configuration = res.configuration
        val metrics: DisplayMetrics = res.displayMetrics

        // Checks Screen Width in dp
        if (config.smallestScreenWidthDp >= DeviceUtilRepo.TABLET_MIN_WIDTH) {
            return true
        }

        // Calculates Physical Screen Size in inches
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        val diagonalInches = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())

        return diagonalInches >= 7.0
    }

    override fun getDataDirectory(): File {
        return context.filesDir
    }

    override fun getAppDirectory(): File? {
        var f: File? = null
        try {
            f = if (buildProviderRepo.getVersionSdkInt() >= 17) {
                File(context.applicationInfo.dataDir)
            } else {
                File("/data/data/" + context.packageName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return f
    }

    override fun getUniqueDeviceId(): String? {
        var android_id: String? = null

        try {
            val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val new_android_id = sharedPreferences.getString(PREF_NEW_LOGGING_ID, null)

            if (new_android_id != null && new_android_id.isNotEmpty()) {
                return new_android_id
            } else {
                //migrate from deprecated pref
                android_id = sharedPreferences.getString(PREF_LOGGING_ID, null)

                if (android_id == null || android_id.equals("unknown", ignoreCase = true) || android_id.isEmpty()) {
                    //deprecated pref is empty or unknown, assign new device id
                    android_id = UUID.randomUUID().toString()
                    Logger.d(TAG, "getUniqueDeviceId(), new id is generated! id=$android_id")
                }
                val editor = sharedPreferences.edit()
                editor.putString(PREF_NEW_LOGGING_ID, android_id)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return android_id
    }

    @SuppressLint("HardwareIds")
    override fun getDeviceSerialId(): String? {
        var android_id: String? = null

        try {
            val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            android_id = sharedPreferences.getString(PREF_DEVICE_SERIAL_ID, null)
            if (android_id == null || buildProviderRepo.getUnknown() == android_id) {
                android_id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                sharedPreferences.edit {
                    putString(PREF_DEVICE_SERIAL_ID, android_id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return android_id
    }

    override fun getDeviceName(): String {
        val manufacturer = buildProviderRepo.getManufacturer()
        val model = buildProviderRepo.getModel()
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }

    private fun capitalize(s: String?): String {
        if (s == null || s.isEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first) + s.substring(1)
        }
    }

    override fun generateJUcid(): String {
        return UUID.randomUUID().toString()
    }

    override fun getLoggingId(): String? {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(PREF_LOGGING_ID, null)
    }

    override fun getNewLoggingId(): String? {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(PREF_NEW_LOGGING_ID, null)
    }

    override fun getNumberOfCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }
}
