package com.washingtonpost.android.config.data.datasources.local

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters
import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.config.domain.models.ConfigProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = ConfigPrefsDataSource.CONFIG_PREFERENCES)

class ConfigPrefsDataSource(
    private val configProvider: ConfigProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val applicationContext: Context = configProvider.applicationContext
    private val stringListAdapter = ConfigMoshiAdapters.stringListAdapter

    suspend fun setConfigOverrides(overrides: List<ConfigOverride>): Boolean =
        withContext(ioDispatcher) {
            try {
                val json = stringListAdapter.toJson(overrides.map { it.name })
                applicationContext.dataStore.edit { preferences ->
                    preferences[CONFIG_OVERRIDES_KEY] = json
                }
                true
            } catch (e: Exception) {
                Logger.e(
                    TAG,
                    "Error saving selected config overrides: ${overrides.joinToString { it.filename }}, error=$e"
                )
                false
            }
        }

    suspend fun getConfigOverrides(): List<ConfigOverride>? = withContext(ioDispatcher) {
        try {
            applicationContext.dataStore.data
                .map { preferences ->
                    val json = preferences[CONFIG_OVERRIDES_KEY]
                    json
                        ?.let { stringListAdapter.fromJson(json) }
                        ?.map { ConfigOverride.valueOf(it) }
                }
                .first()
        } catch (e: Exception) {
            Logger.d(TAG, "Error fetching config overrides, error=$e")
            null
        }
    }

    suspend fun setCurrentVersionCode(versionCode: Int) = withContext(ioDispatcher) {
        configProvider.generalPrefs
            .edit(commit = true) {
                putInt(configProvider.currentVersionCodePrefKey, versionCode)
            }
    }

    fun getCurrentVersionCode(): Int? {
        return try {
            val prefs = configProvider.generalPrefs
            if (!prefs.contains(configProvider.currentVersionCodePrefKey)) return null
            prefs.getInt(configProvider.currentVersionCodePrefKey, -1)
        } catch (e: Exception) {
            Logger.d(TAG, "Error fetching current version code")
            null
        }
    }

    companion object {
        private const val TAG = "ConfigPrefsDataSource"
        const val CONFIG_PREFERENCES = "config_preferences"
        private val CONFIG_OVERRIDES_KEY = stringPreferencesKey("pref.ConfigOverrides")
    }
}