/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.sections.data

import android.content.SharedPreferences
import com.wapo.flagship.features.sections.domein.SectionRibbonRepo
import javax.inject.Inject
import androidx.core.content.edit

class SectionRibbonRepoImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : SectionRibbonRepo {

    override fun getLastViewed(): String? {
        return sharedPreferences.getString(LAST_VIEWED_SECTION_KEY, null)
    }

    override fun setLastViewed(value: String?) {
        sharedPreferences.edit { putString(LAST_VIEWED_SECTION_KEY, value) }
    }

    override fun removeLastViewed() {
        sharedPreferences.edit { remove(LAST_VIEWED_SECTION_KEY) }
    }

    override fun setOpenedOnKey(key: String) {
        sharedPreferences.edit {
            putString(APP_OPENED_ON_KEY, key)
        }
    }

    override fun removeOpenedOnKey() {
        sharedPreferences.edit { remove(APP_OPENED_ON_KEY) }
    }

    companion object {
        // SharedPreferences key for the last viewed section
        val PREFS_NAME = "user_history_prefs"
        val LAST_VIEWED_SECTION_KEY = "last_viewed_section_id"
        val APP_OPENED_ON_KEY = "app_opened_on"
    }
}
