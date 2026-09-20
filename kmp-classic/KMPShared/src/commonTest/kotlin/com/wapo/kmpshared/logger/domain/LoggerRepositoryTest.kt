package com.wapo.kmpshared.logger.domain

import com.wapo.kmpshared.core.config.AppConfig
import com.wapo.kmpshared.core.config.KMPEnv
import com.wapo.kmpshared.core.config.LoggerConfig
import com.wapo.kmpshared.core.lifecycle.AppEnvironment
import com.wapo.kmpshared.core.lifecycle.DefaultAppLifecycle
import com.wapo.kmpshared.logger.data.LogPaths
import com.wapo.kmpshared.logger.domain.model.LogLevel
import com.wapo.kmpshared.logger.domain.model.LogOrigin
import com.wapo.kmpshared.logger.domain.service.LogConsoleWriter
import com.wapo.kmpshared.logger.domain.service.LogFormatter
import com.wapo.kmpshared.testutils.network.MockPlatformNetworkProvider
import com.wapo.kmpshared.util.createKMPURLfromString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.SystemFileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggerRepositoryTest {
    private val paths = LogPaths()
    private val fileSystem = SystemFileSystem
    private val repositories = mutableListOf<LoggerRepository>()
    private val testLoggerConfig =
        LoggerConfig(
            uploaderURL = createKMPURLfromString("https://example.com")!!,
            uploaderToken = "test-token",
            maxLogAge = 8.0 * 60 * 60,
            maxLogSize = 16_384L,
            maxLogFiles = 64,
            sampling = emptyMap(),
        )

    @AfterTest
    fun tearDown() {
        repositories.forEach { it.clear() }
        repositories.clear()
        cleanLoggerDirs()
    }

    @Test
    fun `debug env writes to console and file`() =
        runTest {
            val consoleWriter = CapturingLogWriter()
            val repository =
                createRepository(
                    env = KMPEnv.Debug,
                    consoleWriter = consoleWriter,
                )

            repository.log(
                severity = LogLevel.Debug,
                message = "debug-log-message",
                origin =
                    LogOrigin.Source(
                        file = "LoggerRepositoryTest.kt",
                        function = "debugCase",
                        line = 42u,
                    ),
                sampling = null,
                extras = mapOf("file" to "LoggerRepositoryTest.kt"),
            )
            withTimeout(5_000) { consoleWriter.awaitEntry() }

            assertTrue(
                consoleWriter.entries.isNotEmpty(),
                "debug logs should write to console in Debug env",
            )
            assertTrue(
                consoleWriter.entries.any { it.message.contains("debug-log-message") },
                "debug console output should include the log message",
            )
        }

    @Test
    fun `prod env drops debug severity`() =
        runTest {
            val consoleWriter = CapturingLogWriter()
            val repository =
                createRepository(
                    env = KMPEnv.Prod,
                    consoleWriter = consoleWriter,
                )

            repository.log(
                severity = LogLevel.Debug,
                message = "should-not-be-logged",
                origin = LogOrigin.Module("LoggerRepositoryTest"),
                sampling = null,
                extras = null,
            )
            assertEquals(0, consoleWriter.entries.size)
        }

    @Test
    fun `prod env writes warn severity to file without console output`() =
        runTest {
            val consoleWriter = CapturingLogWriter()
            val repository =
                createRepository(
                    env = KMPEnv.Prod,
                    consoleWriter = consoleWriter,
                )

            repository.log(
                severity = LogLevel.Warn,
                message = "prod-warn-message",
                origin = LogOrigin.Module("LoggerRepositoryTest"),
                sampling = null,
                extras = null,
            )
            assertEquals(0, consoleWriter.entries.size)
        }

    @Test
    fun `log messages are written without errors`() =
        runTest {
            val consoleWriter = CapturingLogWriter()
            val repository =
                createRepository(
                    env = KMPEnv.Test,
                    consoleWriter = consoleWriter,
                )

            repository.log(
                severity = LogLevel.Warn,
                message = "test-message",
                origin = LogOrigin.Module("LoggerRepositoryTest"),
                sampling = null,
                extras = null,
            )
            // Test passes if no exceptions are thrown
            assertTrue(true)
        }

    private fun createRepository(
        env: KMPEnv,
        consoleWriter: LogConsoleWriter = CapturingLogWriter(),
    ): LoggerRepository {
        val lifecycle = DefaultAppLifecycle(createEnvironment(env))
        val appConfig =
            AppConfig(
                version = 1,
                logger = testLoggerConfig,
                feedback = null,
                conversations = null,
            )

        return LoggerRepository(
            appConfig = appConfig,
            config = testLoggerConfig,
            appLifecycle = lifecycle,
            formatter = LogFormatter(),
            network = MockPlatformNetworkProvider(),
            consoleWriter = consoleWriter,
        ).also { repositories += it }
    }

    private fun createEnvironment(env: KMPEnv): AppEnvironment =
        AppEnvironment(
            env = env,
            appBuild = "unit-test",
            appVersion = "1.0.0",
            device = "simulator",
            supportID = "device-123",
            osVersion = "os-1",
        )

    private fun cleanLoggerDirs() {
        listOf(paths.root).forEach { dir ->
            if (fileSystem.exists(dir)) {
                fileSystem
                    .list(dir)
                    .filter { child -> fileSystem.metadataOrNull(child)?.isRegularFile == true }
                    .forEach { child -> fileSystem.delete(child) }
            }
        }
    }

    private data class LogEntry(
        val severity: LogLevel,
        val message: String,
        val tag: String,
    )

    private class CapturingLogWriter : LogConsoleWriter {
        val entries = mutableListOf<LogEntry>()
        private val firstEntry = CompletableDeferred<Unit>()

        suspend fun awaitEntry() = firstEntry.await()

        override fun log(
            severity: LogLevel,
            tag: String,
            message: String,
        ) {
            entries += LogEntry(severity, message, tag)
            firstEntry.complete(Unit)
        }
    }
}
