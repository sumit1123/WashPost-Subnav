/*
 * Copyright (c) 2019. The Washington Post
 */
package com.washingtonpost.android.paywall.reminder

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface ReminderScreenStorage {
    var iapRegistrationAskShownTime: Long
    var iapRegistrationAskReminderShownTime: Long

    fun clearStorage()
}

class ReminderScreenSharedPreferenceStorage private constructor(context: Context) : ReminderScreenStorage {

    private val prefs: Lazy<SharedPreferences> = lazy {
        context.applicationContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE
        )
    }

    override var iapRegistrationAskShownTime by LongPreference(prefs, PREF_IAP_REGISTRATION_ASK_SHOWN_TIME, -1)
    override var iapRegistrationAskReminderShownTime by LongPreference(prefs, PREF_IAP_REGISTRATION_ASK_REMINDER_SHOWN_TIME, -1)

    override fun clearStorage() {
        prefs.value.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "reminder_screen"
        const val PREF_IAP_REGISTRATION_ASK_SHOWN_TIME = "pref_iap_registration_ask_shown_time"
        const val PREF_IAP_REGISTRATION_ASK_REMINDER_SHOWN_TIME = "pref_iap_registration_ask_shown_time"

        @Volatile
        private var INSTANCE: ReminderScreenStorage? = null

        fun getInstance(context: Context): ReminderScreenStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReminderScreenSharedPreferenceStorage(context).also { INSTANCE = it }
            }
        }
    }
}

class LongPreference(
        private val preferences: Lazy<SharedPreferences>,
        private val name: String,
        private val defaultValue: Long
) : ReadWriteProperty<Any, Long> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Long {
        return preferences.value.getLong(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        preferences.value.edit().putLong(name, value).apply()
    }
}