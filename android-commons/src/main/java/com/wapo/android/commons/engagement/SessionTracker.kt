package com.wapo.android.commons.engagement

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class SessionTracker private constructor(
    val context: Context,
    val engagementTracker: EngagementTracker,
    val remoteLog: (message: String) -> Unit,
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var activeActivityCount = 0
    private var lastActiveTime: Long = 0
    private val scope = MainScope()
    private var periodicUpdatesJob: Job? = null
    private var engagementTrace: SessionEngagementTrace? = null
    private val isAppInForeground get() = engagementTrace != null

    init {
        (context as? Application)?.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        checkForInterruptedSession()
    }

    private fun checkForInterruptedSession() {
        val sessionStart = sharedPrefs.getLong(KEY_SESSION_START, 0)
        val lastActiveTime = sharedPrefs.getLong(KEY_LAST_ACTIVE_TIME, 0)
        if (sessionStart > 0) {
            //  Previous session was interrupted (app was killed)
            scope.launch {
                var attempts = 3
                while (attempts > 0) {
                    try {
                        trackEvent(
                            SessionEngagementTrace(
                                id = "previously_killed_session",
                                startTimeMillis = sessionStart,
                                endTimeMillis = lastActiveTime
                            )
                        )
                        remoteLog("Detected an interrupted/killed session: session_start=$sessionStart, last_active_time=$lastActiveTime")
                        return@launch
                    } catch (e: Exception) {
                        Logger.e(TAG, "Error sending previous session engaged time", e)
                        attempts--
                        delay(500)
                    }
                }
            }
            clearSessionState()
        }
    }

    //  region Activity lifecycle

    override fun onActivityStarted(activity: Activity) {
        activeActivityCount++
        updateLastActiveTime()
    }

    override fun onActivityResumed(activity: Activity) {
        updateLastActiveTime()
    }

    override fun onActivityPaused(activity: Activity) {
        updateLastActiveTime()
    }

    override fun onActivityStopped(activity: Activity) {
        activeActivityCount--
        updateLastActiveTime()
        if (activeActivityCount == 0) endSession()
    }

    //  endregion

    //  region Process lifecycle

    override fun onResume(owner: LifecycleOwner) {
        startSession()
    }

    override fun onStop(owner: LifecycleOwner) {
        endSession()
    }

    //  endregion

    //  region Session Management

    private fun startSession() {
        if (isAppInForeground) {
            Logger.w(TAG, "Session has already started")
            return
        }
        engagementTrace = SessionEngagementTrace()
        engagementTrace?.startTrace()
        saveSessionState(engagementTrace?.startTimeMillis ?: -1)
        startPeriodicStateUpdates()
    }

    private fun endSession() {
        engagementTrace?.let { trace ->
            trace.stopTrace()
            trackEvent(trace)
            engagementTrace = null
            clearSessionState()
            stopPeriodicStateUpdates()
        }
    }

    private fun saveSessionState(startTime: Long) {
        sharedPrefs.edit {
            putLong(KEY_SESSION_START, startTime)
            putLong(KEY_LAST_ACTIVE_TIME, startTime)
        }
    }

    private fun clearSessionState() {
        sharedPrefs.edit {
            remove(KEY_SESSION_START)
            remove(KEY_LAST_ACTIVE_TIME)
        }
    }

    private fun updateLastActiveTime() {
        lastActiveTime = System.currentTimeMillis()
        if (isAppInForeground) {
            sharedPrefs.edit {
                putLong(KEY_LAST_ACTIVE_TIME, lastActiveTime)
            }
        }
    }

    private fun startPeriodicStateUpdates() {
        periodicUpdatesJob?.cancel()
        periodicUpdatesJob = scope.launch {
            while (true) {
                if (isAppInForeground) {
                    updateLastActiveTime()
                }
                delay(SESSION_PERSIST_INTERVAL_MS)
            }
        }
    }

    private fun stopPeriodicStateUpdates() {
        periodicUpdatesJob?.cancel()
    }

    //  endregion

    //  region Analytics

    private fun trackEvent(trace: EngagementTrace) {
        engagementTracker.trackEngagement(trace)
    }

    //  endregion

    //  region Unused lifecycle events

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    //  endregion

    companion object {
        private const val TAG = "SessionTracker"
        private const val PREFS_NAME = "session_tracker"
        private const val KEY_SESSION_START = "session_start_time"
        private const val KEY_LAST_ACTIVE_TIME = "last_activity_time"
        private const val SESSION_PERSIST_INTERVAL_MS = 10_000L

        @Volatile
        private lateinit var instance: SessionTracker

        fun init(
            appContext: Context,
            engagementTracker: EngagementTracker,
            remoteLog: (message: String) -> Unit,
        ) {
            if (!::instance.isInitialized) {
                instance = SessionTracker(appContext, engagementTracker, remoteLog)
            }
        }

    }
}