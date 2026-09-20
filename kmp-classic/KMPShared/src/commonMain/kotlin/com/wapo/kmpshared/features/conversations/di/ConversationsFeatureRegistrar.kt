package com.wapo.kmpshared.features.conversations.di

import com.wapo.kmpshared.core.config.ConversationsConfig
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ksp.generated.module

internal class ConversationsFeatureRegistrar(
    private val koin: Koin,
) {
    private var config: ConversationsConfig? = null
    private var activeModule: Module? = null

    fun apply(config: ConversationsConfig?) {
        if (this.config == config) return

        runCatching { activeModule?.let { koin.unloadModules(listOf(it)) } }

        if (config != null) {
            val nextModule = module {
                single<ConversationsConfig> { config }
                includes(ConversationsModule().module)
            }

            try {
                koin.loadModules(listOf(nextModule))

                this.activeModule = nextModule
                this.config = config
            } catch (_: Exception) {
                // Defensive unload for completeness, matching PR feedback
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
