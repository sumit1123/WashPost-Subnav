package com.washingtonpost.android.config.domain.models.config

import android.content.Context
import android.os.Build
import android.util.Log
import com.wapo.android.commons.util.Logger

data class VersionConfig(
    val enabled: Boolean,
    val versionCodesToUpdate: List<Int>,
    val forceUpdate: Boolean,
    val minSdk: Int,
    val minSdkMessage: String,
) {
    fun isOsUpgradeRequired(): Boolean {
        val sdkVersion = Build.VERSION.SDK_INT
        return sdkVersion < minSdk
    }

    fun isAppToBeUpdated(context: Context) =
        isAppToBeUpdatedAndForced(context).first

    fun isAppToBeForceUpdated(context: Context) =
        isAppToBeUpdatedAndForced(context).second

    private fun isAppToBeUpdatedAndForced(context: Context): Pair<Boolean, Boolean> {
        try {
            if (!enabled) return Pair(false, false)
            val currentVersion =
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            val isAppUpdateAvailable = versionCodesToUpdate.contains(currentVersion)
            Logger.d(
                TAG,
                "isAppToBeUpdatedAndForced: " +
                        "currentVersion: $currentVersion, " +
                        "versionCodesToUpdate: $versionCodesToUpdate, " +
                        "updateAvailable: $isAppUpdateAvailable, " +
                        "forceUpdate: $forceUpdate",
            )
            return Pair(isAppUpdateAvailable, isAppUpdateAvailable && forceUpdate)
        } catch (e: Exception) {
            Log.e(TAG, "isAppToBeUpdatedAndForced exception ", e)
            return Pair(false, false)
        }
    }

    companion object {
        private val TAG = VersionConfig::class.simpleName
    }
}