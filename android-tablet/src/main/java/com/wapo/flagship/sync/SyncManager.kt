package com.wapo.flagship.sync

import android.accounts.AccountManager
import android.content.Context
import android.os.SystemClock
import com.wapo.android.commons.util.Logger
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.settings.AppPreferences
import com.washingtonpost.android.config.domain.manager.ConfigManager
import java.util.concurrent.TimeUnit

const val SYNC_WORK_NAME = "sync"
const val TAG = "SyncManager"
private const val SYNC_PREF_NAME = "work_manager"
private const val PREF_LAST_SYNC_TIME = "last_sync_time"

class SyncManager(
    private val context: Context,
) {
    fun setPeriodicSync(
        interval: Long,
        timeUnit: TimeUnit,
    ) {
        Logger.d(TAG, "setupSync $interval $timeUnit")
        if (interval > 0) {
            val builder = OneTimeWorkRequest.Builder(SyncWorker::class.java)

            val lastSyncTime = getLastSyncTime()
            val currentTime = SystemClock.elapsedRealtime()
            val syncIntervalMs = timeUnit.toMillis(interval)
            if (lastSyncTime != 0L && currentTime >= lastSyncTime) {
                val timePassed = currentTime - lastSyncTime
                val nextSyncTimeDelay = syncIntervalMs - timePassed
                if (nextSyncTimeDelay > 0) {
                    Logger.d(TAG, "setting delay to ${nextSyncTimeDelay / 1000} SECONDS")
                    builder.setInitialDelay(nextSyncTimeDelay, TimeUnit.MILLISECONDS)
                }
            }

            val workRequest = builder.build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        }
    }

    fun saveLastSyncTime(time: Long = SystemClock.elapsedRealtime()) {
        context
            .getSharedPreferences(SYNC_PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(PREF_LAST_SYNC_TIME, time)
            .apply()
    }

    fun getLastSyncTime(): Long =
        context
            .getSharedPreferences(SYNC_PREF_NAME, Context.MODE_PRIVATE)
            .getLong(PREF_LAST_SYNC_TIME, 0)

    fun removeSyncAccount() {
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager?.getAccountsByType("com.washingtonpost.android.Account")
            if (accounts != null) {
                for (account in accounts) {
                    accountManager.removeAccount(account, null, null)
                }
            }
        } catch (t: Throwable) {
            Logger.d(TAG, "remove account error", t)
        }
    }

    fun requestSync() {
        saveLastSyncTime(0L)
        setPeriodicSync(1, TimeUnit.SECONDS)
    }

    companion object {
        private const val FOREGROUND_DEFAULT_SYNC_TIME_SEC = 60L

        fun reschedule(
            foreground: Boolean = FlagshipApplication.isInForeground,
            context: Context,
        ) {
            if (foreground) {
                val foregroundSyncTime =
                    ConfigManager.getInstance()
                        .config
                        .contentUpdateRulesConfig
                        .foregroundSyncTimeInSeconds
                        .coerceAtLeast(FOREGROUND_DEFAULT_SYNC_TIME_SEC)
                SyncManager(context).setPeriodicSync(foregroundSyncTime, TimeUnit.SECONDS)
            } else {
                SyncManager(context).setPeriodicSync(
                    AppPreferences.getBackgroundSyncAsInt().toLong(),
                    TimeUnit.SECONDS,
                )
            }
        }
    }
}
