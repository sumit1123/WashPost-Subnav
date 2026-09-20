package com.wapo.kmpshared.features.feedback

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.config.FeedbackServiceConfig
import com.wapo.kmpshared.core.config.KMPEnv
import com.wapo.kmpshared.core.di.Qualifiers
import com.wapo.kmpshared.core.lifecycle.AppEnvironment
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.core.lifecycle.AppLifecycleChange
import com.wapo.kmpshared.core.lifecycle.DefaultAppLifecycle
import com.wapo.kmpshared.core.lifecycle.ReachabilityStatus
import com.wapo.kmpshared.core.lifecycle.ReachabilityType
import com.wapo.kmpshared.core.network.FeatureNetworkConfig
import com.wapo.kmpshared.core.network.NetworkClient
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.features.feedback.data.FeedbackService
import com.wapo.kmpshared.features.feedback.data.RemoteFeedbackService
import com.wapo.kmpshared.features.feedback.domain.FeedbackRepository
import com.wapo.kmpshared.features.feedback.domain.model.Feedback
import com.wapo.kmpshared.features.feedback.domain.model.FeedbackSurface
import com.wapo.kmpshared.logger.WPLogger
import com.wapo.kmpshared.logger.domain.LoggerProvider
import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogOrigin
import com.wapo.kmpshared.testutils.network.MockPlatformNetworkProvider
import com.wapo.kmpshared.util.createKMPURLfromString
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs

class FeedbackRepositoryTest : KoinTest {
    private val testFeedback =
        Feedback(
            surface = FeedbackSurface.PersonalizedPodcast("unit-test-id", 0.0),
            rating = 1,
            comment = "Test comment",
        )

    private val testAppEnvironment =
        AppEnvironment(
            env = KMPEnv.Test,
            appBuild = null,
            appVersion = null,
            device = null,
            supportID = "0000-0000-0000",
            osVersion = "",
        )

    private fun setupTestKoin(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
        val mockEngine = MockEngine(handler)
        val feedbackConfig =
            FeedbackServiceConfig(
                baseURL = requireNotNull(createKMPURLfromString("https://example.com/feedback")),
            )
        val appLifecycle =
            DefaultAppLifecycle(testAppEnvironment).apply {
                receiveChange(
                    AppLifecycleChange.Reachability(
                        ReachabilityStatus.Reachable(
                            ReachabilityType.Wifi,
                            isExpensive = false,
                            isConstrained = false,
                        ),
                    ),
                )
            }
        startKoin {
            modules(
                module {
                    single { Json { ignoreUnknownKeys = true } }
                    single<CoroutineScope>(named(Qualifiers.APPLICATION_SCOPE)) {
                        CoroutineScope(SupervisorJob() + Dispatchers.Default)
                    }
                    single<PlatformNetworkProvider> { MockPlatformNetworkProvider() }
                    single<FeedbackServiceConfig> { feedbackConfig }
                    single<AppLifecycle> { appLifecycle }
                    single { NetworkClient(mockEngine, get(), get()) }
                    single<HttpClient>(named(Qualifiers.FEEDBACK)) {
                        get<NetworkClient>().forFeature(FeatureNetworkConfig.FEEDBACK)
                    }
                    single<LoggerProvider> {
                        object : LoggerProvider {
                            override fun log(
                                severity: LogLevel,
                                message: String,
                                origin: LogOrigin,
                                sampling: String?,
                                extras: Map<Any, Any?>?,
                            ) = Unit

                            override fun upload() = Unit
                        }
                    }
                    single { WPLogger() }

                    single<FeedbackService> {
                        RemoteFeedbackService(
                            get(named(Qualifiers.APPLICATION_SCOPE)),
                            get(named(Qualifiers.FEEDBACK)),
                            get(),
                            get(),
                            get(),
                            get(),
                            get(),
                        )
                    }
                    single { FeedbackRepository(get()) }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `test success 200`() =
        runTest {
            setupTestKoin { respond("", status = HttpStatusCode.OK) }
            val result = get<FeedbackRepository>().submitFeedback(testFeedback)
            assertIs<KMPResult.Success<Unit>>(result)
        }

    @Test
    fun `test error 404`() =
        runTest {
            setupTestKoin { respond("", status = HttpStatusCode.NotFound) }
            val result = get<FeedbackRepository>().submitFeedback(testFeedback)
            assertIs<KMPResult.Error>(result)
        }

    @Test
    fun `test error 500`() =
        runTest {
            setupTestKoin { respond("", status = HttpStatusCode.InternalServerError) }
            val result = get<FeedbackRepository>().submitFeedback(testFeedback)
            assertIs<KMPResult.Error>(result)
        }

    @Test
    fun `test network failure`() =
        runTest {
            setupTestKoin { throw Exception("Network Connection Failed") }
            val result = get<FeedbackRepository>().submitFeedback(testFeedback)
            assertIs<KMPResult.Error>(result)
        }
}
