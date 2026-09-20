package com.washingtonpost.android.config.domain.models

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.content.res.Resources
import com.wapo.android.commons.logs.EventLog
import kotlinx.coroutines.CoroutineScope
import java.io.File

interface ConfigProvider {
    val configScope: CoroutineScope
    val applicationContext: Context
    val resources: Resources get() = applicationContext.resources
    val assets: AssetManager get() = applicationContext.assets
    val filesDir: File get() = applicationContext.filesDir
    val generalPrefs: SharedPreferences

    //  Build type
    val storeType: StoreType
    val isDebugBuild: Boolean
    val isTablet: Boolean

    //  Configs from application module
    val appVersionCode: Int
    val deviceUniqueId: String
    val deviceSerialId: String
    val archiveDirectory: String
    val canStoreRemoteLogs: Boolean
    val currentVersionCodePrefKey: String
    val packageName: String

    fun remoteLogError(eventLog: EventLog)
    fun decryptSecureData(value: String): String?
}
