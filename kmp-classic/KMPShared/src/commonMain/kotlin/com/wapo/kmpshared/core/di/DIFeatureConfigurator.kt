package com.wapo.kmpshared.core.di

import com.wapo.kmpshared.core.config.AppConfig
import com.wapo.kmpshared.features.conversations.di.ConversationsFeatureRegistrar
import com.wapo.kmpshared.features.feedback.di.FeedbackFeatureRegistrar
import com.wapo.kmpshared.logger.di.LoggerRegistrar
import com.wapo.kmpshared.logger.domain.LoggerProvider
import org.koin.core.Koin

class DIFeatureConfigurator(
    private val koin: Koin,
    logger: LoggerProvider,
) {
    private val logger = LoggerRegistrar(koin, logger)
    private val conversations = ConversationsFeatureRegistrar(koin)
    private val feedback = FeedbackFeatureRegistrar(koin)

    fun apply(config: AppConfig) {
        koin.declare(config, allowOverride = true)
        logger.apply(config)
        conversations.apply(config.conversations)
        feedback.apply(config.feedback)
    }

    fun tearDown() {
        logger.tearDown()
    }
}
