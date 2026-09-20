package com.wapo.kmpshared.core.di

import com.wapo.kmpshared.core.config.AppConfig
import com.wapo.kmpshared.core.config.KMPEnv
import com.wapo.kmpshared.core.lifecycle.AppEnvironment
import com.wapo.kmpshared.core.lifecycle.DefaultAppLifecycle
import com.wapo.kmpshared.logger.domain.LoggerProvider
import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogOrigin
import com.wapo.kmpshared.testutils.network.MockPlatformNetworkProvider
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.Test

class KoinGraphValidationTest : KoinTest {
    @AfterTest
    fun tearDown() {
        DIFactory.tearDown()
    }

    @Test
    fun checkKoinGraph() {
        val appConfig =
            AppConfig(
                feedback = null,
                conversations = null,
                version = 1,
                logger = null,
            )

        DIFactory.setUp(
            network = MockPlatformNetworkProvider(),
            lifecycle =
                DefaultAppLifecycle(
                    env =
                        AppEnvironment(
                            env = KMPEnv.Test,
                            appBuild = null,
                            appVersion = null,
                            device = null,
                            supportID = "0000-0000-0000",
                            osVersion = "",
                        ),
                ),
            config = appConfig,
            logger = noOpLogger,
        )
        // If we got here without exception, the graph was set up successfully
    }

    companion object {
        val noOpLogger =
            object : LoggerProvider {
                override fun log(
                    severity: LogLevel,
                    message: String,
                    origin: LogOrigin,
                    sampling: String?,
                    extras: Map<Any, Any?>?,
                ) {
                }

                override fun upload() {
                }
            }
    }
}
