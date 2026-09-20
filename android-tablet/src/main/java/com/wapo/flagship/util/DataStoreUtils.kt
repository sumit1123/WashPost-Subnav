// Copyright (c) 2024 The Washington Post. All rights reserved.
@file:JvmName("DataStoreUtils")

package com.wapo.flagship.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wapo.flagship.FlagshipApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val context = FlagshipApplication.getInstance()
internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences",
)

// FAILOVER_ACTIVE
private val FAILOVER_ACTIVE = booleanPreferencesKey("failover_active")
var isFailoverActive: Flow<Boolean> = MutableStateFlow(false)
    get() {
        return context.dataStore.data.map { it[FAILOVER_ACTIVE] ?: field.first() }
    }

suspend fun setFailoverActive(value: Boolean) {
    context.dataStore.edit { it[FAILOVER_ACTIVE] = value }
}

// NEWSPRINT_ENGAGED_STATUS
private val NEWSPRINT_ENGAGED_STATUS = stringPreferencesKey("newsprint_engaged_status")
var newsprintEngagedStatus: Flow<String> = MutableStateFlow("")
    get() {
        return context.dataStore.data.map { it[NEWSPRINT_ENGAGED_STATUS] ?: field.first() }
    }

suspend fun setNewsprintEngagedStatus(value: String) {
    context.dataStore.edit { it[NEWSPRINT_ENGAGED_STATUS] = value }
}

// NEWSPRINT_HAS_VIEWED
private val NEWSPRINT_HAS_VIEWED = booleanPreferencesKey("newsprint_has_viewed")
var newsprintHasViewed: Flow<Boolean> = MutableStateFlow(false)
    get() {
        return context.dataStore.data.map { it[NEWSPRINT_HAS_VIEWED] ?: field.first() }
    }

suspend fun setNewsprintHasViewed(value: Boolean) {
    context.dataStore.edit { it[NEWSPRINT_HAS_VIEWED] = value }
}

// NEWSPRINT_READER_TYPE
private val NEWSPRINT_READER_TYPE = stringPreferencesKey("newsprint_reader_type")
var newsprintReaderType: Flow<String> = MutableStateFlow("")
    get() {
        return context.dataStore.data.map { it[NEWSPRINT_READER_TYPE] ?: field.first() }
    }

suspend fun setNewsprintReaderType(value: String) {
    context.dataStore.edit { it[NEWSPRINT_READER_TYPE] = value }
}
