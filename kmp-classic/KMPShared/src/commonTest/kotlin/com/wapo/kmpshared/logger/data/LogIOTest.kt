package com.wapo.kmpshared.logger.data

import com.wapo.kmpshared.core.config.LoggerConfig
import com.wapo.kmpshared.testutils.network.MockPlatformNetworkProvider
import com.wapo.kmpshared.util.createKMPURLfromString
import com.wapo.kmpshared.util.div
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogIOTest {
    private val paths by lazy { LogPaths() }
    private val fileSystem = SystemFileSystem
    private val uploadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val testConfig =
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
        uploadScope.cancel()
        cleanLoggerDirs()
    }

    @Test
    fun `write appends small messages without rotation`() {
        val logIO = createLogIO()

        logIO.write("small-message")
        logIO.flush()

        val logs = fileSystem.list(paths.root).filter { it.name.endsWith(".log") }
        assertEquals(1, logs.size, "Should have exactly one active log file")
    }

    @Test
    fun `write creates file successfully`() {
        val logIO = createLogIO()

        logIO.write("test-message")
        logIO.flush()

        assertTrue(fileSystem.list(paths.root).isNotEmpty(), "write should create a file")
    }

    @Test
    fun `write handles multiple messages`() {
        val logIO = createLogIO()

        logIO.write("message-1")
        logIO.write("message-2")
        logIO.write("message-3")
        logIO.flush()

        val logs = fileSystem.list(paths.root).filter { it.name.endsWith(".log") }
        assertEquals(1, logs.size, "Multiple small writes should stay in the same file")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `rotate and compress leaves next file creation lazy`() =
        runTest {
            val config = testConfig.copy(maxLogAge = 0.0)
            val logIO = createLogIO(config, this)

            logIO.write("background-message")
            advanceUntilIdle()

            // Since rotation happens immediately and compression deletes the source,
            // no .log file should remain until the next write.
            assertFalse(fileSystem.list(paths.root).any { it.name.endsWith(".log") })

            logIO.clear()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `compressAndUpload keeps latest log active`() =
        runTest {
            createLogFile("log-1.log")
            createLogFile("log-2.log")
            val logIO = createLogIO(uploadScope = this)

            logIO.compressAndUpload()
            advanceUntilIdle()

            val remainingLogs =
                fileSystem
                    .list(paths.root)
                    .filter { it.name.endsWith(".log") }
                    .map { it.name }

            assertEquals(listOf("log-2.log"), remainingLogs)

            logIO.clear()
        }

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

    private fun createLogFile(name: String) {
        if (!fileSystem.exists(paths.root)) fileSystem.createDirectories(paths.root)
        fileSystem
            .sink(paths.root / name)
            .buffered()
            .use { it.writeString("test-message\n") }
    }

    private fun createLogIO(
        config: LoggerConfig = testConfig,
        uploadScope: CoroutineScope = this.uploadScope,
    ): LogIO =
        LogIO(
            config = config,
            network = MockPlatformNetworkProvider(),
            uploadScope = uploadScope,
        )
}
