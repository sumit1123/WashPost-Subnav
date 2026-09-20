package com.washingtonpost.android.config.data.datasources.utils

import android.content.res.AssetManager
import android.content.res.Resources
import com.washingtonpost.android.config.domain.models.ConfigProvider
import java.io.InputStream

data class MapConfigParams(
    private val cryptoHelper: ConfigCryptoHelper,
    val configProvider: ConfigProvider,
) {
    val archiveDirectory = configProvider.archiveDirectory
    val canStoreRemoteLogs = configProvider.canStoreRemoteLogs
    val deviceUniqueId = configProvider.deviceUniqueId
    val deviceSerialId = configProvider.deviceSerialId
    val isTablet: Boolean = configProvider.isTablet
    val packageName: String = configProvider.packageName

    val assets: AssetManager = configProvider.assets
    val resources: Resources = configProvider.resources
    val filesDirectoryAbsolutePath: String = configProvider.filesDir.absolutePath

    fun decrypt(value: String): String {
        return cryptoHelper.decryptValue(value)
    }

    fun decryptSecureData(value: String): String? {
        return configProvider.decryptSecureData(value)
    }

    inline fun <reified T> getDefaultConfigFromResource(filename: String): T? {
        return try {
            val inputStream: InputStream = assets.open(filename)
            val json = inputStream.bufferedReader().use { it.readText() }
            ConfigMoshiAdapters.moshi
                .adapter(T::class.java)
                .fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
