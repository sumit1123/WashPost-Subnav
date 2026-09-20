package com.wapo.flagship

import android.content.Context
import com.wapo.android.commons.config.ConfigManager
import com.wapo.android.commons.config.Constants
import com.wapo.flagship.content.ContentManager
import rx.Observable
import java.util.concurrent.TimeUnit

object ConfigSyncObservableFactory {
    fun createConfigSyncObservable(
        context: Context,
        configType: Constants.ConfigType,
        configManager: ConfigManager,
        configFailureListener: ConfigManager.ConfigFailureListener
    ): Observable<ContentManager.ConfigSyncOpInfo> {
        configManager.loadRemoteConfig(context, configType, configFailureListener)
        return configManager
            .getConfigSubjectOfType(configType)
            .skip(1) // skip the cached value and wait for the next one
            .asObservable()
            .map { ContentManager.ConfigSyncOpInfo() }
            .take(1)
            .timeout(5, TimeUnit.SECONDS)
            .onErrorReturn { ContentManager.ConfigSyncOpInfo() }
    }
}
