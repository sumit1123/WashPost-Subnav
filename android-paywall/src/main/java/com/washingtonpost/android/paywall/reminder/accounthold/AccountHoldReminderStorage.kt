package com.washingtonpost.android.paywall.reminder.accounthold

import android.content.Context
import android.content.SharedPreferences
import com.washingtonpost.android.paywall.reminder.LongPreference
import com.washingtonpost.android.paywall.reminder.ReminderScreenStorage

class AccountHoldReminderStorage private constructor(context: Context) : ReminderScreenStorage {
    private val prefs: Lazy<SharedPreferences> = lazy {
        context.applicationContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE
        )
    }

    override var iapRegistrationAskShownTime by LongPreference(prefs, PREF_ACQ_FIRST_SHOWN_TIME, -1)
    override var iapRegistrationAskReminderShownTime by LongPreference(prefs, PREF_ACQ_REMINDER_SHOWN_TIME, -1)

    override fun clearStorage() {
        prefs.value.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "acc_hold_reminder_screen"
        const val PREF_ACQ_FIRST_SHOWN_TIME = "pref_acc_hold_reminder_shown_time"
        const val PREF_ACQ_REMINDER_SHOWN_TIME = "pref_acc_hold_reminder_shown_time"

        @Volatile
        private var INSTANCE: AccountHoldReminderStorage? = null

        fun getInstance(context: Context): AccountHoldReminderStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AccountHoldReminderStorage(context).also { INSTANCE = it }
            }
        }
    }
}