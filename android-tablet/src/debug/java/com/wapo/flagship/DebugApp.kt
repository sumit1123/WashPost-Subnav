package com.wapo.flagship

import android.os.Build
import com.wapo.flagship.util.PrefUtils
import leakcanary.AppWatcher
import leakcanary.LeakCanary
import leakcanary.RootViewWatcher

class DebugApp : FlagshipApplication() {
    override fun onCreate() {
        super.onCreate()

        val watchers = AppWatcher.appDefaultWatchers(this)
            .filterNot {
                it is RootViewWatcher && Build.MANUFACTURER.equals(
                    "samsung",
                    ignoreCase = true
                )
            }
        AppWatcher.manualInstall(
            application = this,
            watchersToInstall = watchers,
        )

        LeakCanary.config =
            LeakCanary.config.copy(dumpHeap = PrefUtils.getPrefLeakCanaryDumpHeap(this))
    }
}
