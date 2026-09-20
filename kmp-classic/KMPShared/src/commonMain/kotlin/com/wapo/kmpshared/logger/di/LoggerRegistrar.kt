package com.wapo.kmpshared.logger.di

import com.wapo.kmpshared.core.config.AppConfig
import com.wapo.kmpshared.core.config.LoggerConfig
import com.wapo.kmpshared.logger.WPLogger
import com.wapo.kmpshared.logger.domain.LoggerProvider
import com.wapo.kmpshared.logger.domain.LoggerRepository
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ksp.generated.module

internal class LoggerRegistrar(
    private val koin: Koin,
    private val platformLogger: LoggerProvider,
) {
    private var config: LoggerConfig? = null
    private var activeModule: Module? = null

    private fun platformModule() =
        module {
            single<LoggerProvider> { platformLogger }
        }

    private fun kmpModule(config: LoggerConfig) =
        module {
            single<LoggerConfig> { config }
            includes(LoggerModule().module)
        }

    fun apply(appConfig: AppConfig) {
        try {
            val config = appConfig.logger
            if (activeModule != null && this.config == config) {
                koin.getOrNull<LoggerRepository>()?.updateAppConfig(appConfig)
                return
            }

            val nextModule = if (config != null) kmpModule(config) else platformModule()

            // Logger reconfiguration is not atomic for callers of WPLogger. Logs emitted
            // after the old provider is cleared and before the new provider is installed
            // may be dropped.
            runCatching { koin.getOrNull<LoggerRepository>()?.clear() }
            runCatching { activeModule?.let { koin.unloadModules(listOf(it)) } }

            koin.loadModules(listOf(nextModule), allowOverride = true)

            koin.get<WPLogger>().updateProvider(koin.get<LoggerProvider>())

            this.config = config
            this.activeModule = nextModule
        } catch (_: Exception) {
            runCatching { activeModule?.let { koin.unloadModules(listOf(it)) } }
            runCatching { koin.get<WPLogger>().updateProvider(platformLogger) }
            this.activeModule = null
            this.config = null
        }
    }

    fun tearDown() {
        if (config != null) {
            runCatching { koin.getOrNull<LoggerRepository>()?.clear() }
        }
    }
}
