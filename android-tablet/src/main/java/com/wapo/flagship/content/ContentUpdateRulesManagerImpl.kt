package com.wapo.flagship.content

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.content.ContentUpdateRulesManager.*
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.config.domain.models.config.ContentUpdateRulesConfig

class ContentUpdateRulesManagerImpl(
    val context: Context,
    private val contentRefreshConfig: ContentUpdateRulesConfig?,
) : ContentUpdateRulesManager {
    private val TAG = "ContentUpdateRules"
    private val prefsFileName: String = (javaClass.`package`?.toString() ?: context.packageName) + ".cache.prefs"
    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            prefsFileName,
            Context.MODE_PRIVATE,
        )

    override fun clearTimes() {
        prefs.edit().clear().apply()
    }

    override fun getTime(
        timeType: TimeType,
        key: String?,
    ): Long {
        val prefKey = timeType.name + (key ?: "")
        return prefs.getLong(prefKey, 0)
    }

    override fun setTime(
        timeType: TimeType,
        key: String?,
        value: Long,
    ) {
        val prefKey = timeType.name + (key ?: "")
        val prefsEditor = prefs.edit()
        prefsEditor.putLong(prefKey, value)
        prefsEditor.apply()
        if (BuildConfig.DEBUG) {
            Logger.d(TAG, "SyncRules: $timeType, $key, $value")
        }
    }

    override fun setTime(
        timeType: TimeType,
        key: String?,
    ) {
        setTime(timeType, key, SystemClock.elapsedRealtime())
    }

    override fun refreshNeeded(path: String?): Boolean {
        if (path != null) {
            return getTime(TimeType.APP_RESUME_TIME) > getTime(TimeType.PAGE_STOP_TIME, path)
        }
        return false
    }

    override fun shouldConsiderCache(path: String?): Boolean {
        if (contentRefreshConfig != null && path != null) {
            return SystemClock.elapsedRealtime() - getTime(TimeType.PAGE_STOP_TIME, path) <=
                contentRefreshConfig.sectionsColdUpdatesInterval
        }
        return false
    }

    override fun doesContentNeedRefresh(time: Long?): Boolean {
        if (contentRefreshConfig != null && time != null) {
            return (System.currentTimeMillis() - time) > contentRefreshConfig.sectionsColdUpdatesInterval
        }
        return false
    }
}
