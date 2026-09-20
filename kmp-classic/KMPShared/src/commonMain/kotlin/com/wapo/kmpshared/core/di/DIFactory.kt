// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.di

import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.wapo.kmpshared.core.config.AppConfig
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.core.network.di.NativeNetworkModule
import com.wapo.kmpshared.core.network.di.NetworkModule
import com.wapo.kmpshared.logger.WPLogger
import com.wapo.kmpshared.logger.domain.LoggerProvider
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

object DIFactory {
    private val lock = Lock()
    private var configurator: DIFeatureConfigurator? = null

    fun setUp(
        lifecycle: AppLifecycle,
        logger: LoggerProvider,
        network: PlatformNetworkProvider,
        config: AppConfig,
    ) {
        try {
            lock.withLock {
                if (configurator != null) return@withLock

                val koinApp =
                    startKoin { modules(
                        module {
                            single<PlatformNetworkProvider> { network }
                            single<AppLifecycle> { lifecycle }
                            single<AppConfig> { config }
                        },
                        module {
                            single<WPLogger> { WPLogger() }
                        },
                        NativeNetworkModule().module,
                        NetworkModule().module,
                        ScopeModule().module,
                    ) }

                configurator = DIFeatureConfigurator(koinApp.koin, logger)

                configurator?.apply(config)
            }
        } catch (_: Exception) {
            tearDown()
        }
    }

    fun apply(config: AppConfig) {
        lock.withLock {
            runCatching { configurator?.apply(config) }
        }
    }

    fun tearDown() {
        lock.withLock {
            runCatching { configurator?.tearDown() }
            runCatching { stopKoin() }
            configurator = null
        }
    }
}
