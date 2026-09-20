package com.wapo.flagship.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.EventTimerLog
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.content.ContentManager.SyncOpInfo
import com.wapo.flagship.util.ConnectivityMonitor
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.wrappers.CrashWrapper
import rx.Subscriber

class SyncWorker(
    val context: Context,
    workerParams: WorkerParameters,
) : Worker(
        context,
        workerParams,
    ) {
    override fun doWork(): Result =
        try {
            sync()
            Result.success()
        } catch (t: Throwable) {
            Logger.d(TAG, "sync error ${t.localizedMessage}")
            CrashWrapper.sendException(t)
            Result.failure()
        } finally {
            FlagshipApplication.getInstance().syncManager.saveLastSyncTime()
            SyncManager.reschedule(FlagshipApplication.isInForeground, context)
        }

    private fun sync() {
        Logger.d(TAG, "sync")
        val hasConnection = ReachabilityUtil.isConnected(context)
        val isAppRunning = FlagshipApplication.isInForeground
        val isAllowingBackgroundSync =
            AppContext.isAllowingBackgroundSync(context) &&
                !ConnectivityMonitor.getInstance(context).hasDeviceLevelDataRestriction()
        val syncNow =
            (isAppRunning || isAllowingBackgroundSync) &&
                FlagshipApplication.getInstance().canRunSync() &&
                hasConnection

        Logger.d(TAG, "sync now: $syncNow")
        if (syncNow) {
            EventTimerLog.restartTimingEvent(
                EventTimerLog.SYNC_EVENT,
                EventTimerLog.ALL_SECTION_REFRESH,
            )
            FlagshipApplication
                .getInstance()
                .contentManager
                .performPagesSync(null)
                .toBlocking()
                .subscribe(
                    object : Subscriber<SyncOpInfo?>() {
                        override fun onCompleted() {
                            EventTimerLog.stopTimingEventAndLog(
                                EventTimerLog.SYNC_EVENT,
                                EventTimerLog.ALL_SECTION_REFRESH,
                                context,
                                false,
                                EventTimerLog.ALL_SECTION_REFRESH,
                            )
                        }

                        override fun onError(e: Throwable) {
                            EventTimerLog.stopTimingEvent(
                                EventTimerLog.SYNC_EVENT,
                                EventTimerLog.ALL_SECTION_REFRESH,
                            )
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("Sync Error")
                                    setModule(LogModules.SYNC)
                                    setErrorMessage(e.message)
                                }.run {
                                    RemoteLog.e(context, build())
                                }
                            CrashWrapper.sendException(e)
                        }

                        override fun onNext(t: SyncOpInfo?) {}
                    },
                )
        }

        Logger.d(TAG, "sync completed")
    }
}
