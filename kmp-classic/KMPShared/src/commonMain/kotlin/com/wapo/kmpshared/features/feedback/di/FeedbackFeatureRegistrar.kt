package com.wapo.kmpshared.features.feedback.di

import com.wapo.kmpshared.core.config.FeedbackServiceConfig
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ksp.generated.module

internal class FeedbackFeatureRegistrar(
    private val koin: Koin,
) {
    private var config: FeedbackServiceConfig? = null
    private var activeModule: Module? = null

    fun apply(config: FeedbackServiceConfig?) {
        if (this.config == config) return

        runCatching { activeModule?.let { koin.unloadModules(listOf(it)) } }

        if (config != null) {
            val nextModule = module {
                single<FeedbackServiceConfig> { config }
                includes(FeedbackModule().module)
            }

            try {
                koin.loadModules(listOf(nextModule))

                this.activeModule = nextModule
                this.config = config
            } catch (_: Exception) {
                runCatching { koin.unloadModules(listOf(nextModule)) }

                this.activeModule = null
                this.config = null
            }
        } else {
            this.activeModule = null
            this.config = null
        }
    }
}
