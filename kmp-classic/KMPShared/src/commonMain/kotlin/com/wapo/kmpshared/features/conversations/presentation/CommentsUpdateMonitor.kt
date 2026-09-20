package com.wapo.kmpshared.features.conversations.presentation

import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.concurrency.withLock
import com.wapo.kmpshared.features.conversations.data.CommentsRepository
import com.wapo.kmpshared.util.KMPURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class CommentsUpdateMonitor(
    private val repo: CommentsRepository,
) {
    private val monitorJob = SupervisorJob()
    private val monitorScope = CoroutineScope(Dispatchers.Main + monitorJob)
    private val _hasUpdates = MutableStateFlow(false)
    val hasUpdates = _hasUpdates.asStateFlow()

    private val storyURL = AtomicReference<KMPURL?>(null)
    private val lastKnownTopId = AtomicReference<String?>(null)

    private val pollingJob = AtomicReference<Job?>(null)
    private val lock = Lock()

    /**
     * Starts a polling loop that checks for new comments every 60 seconds.
     * * Uses this monitor's internal `Dispatchers.Main` scope for the polling loop.
     */
    fun startMonitoring(
        url: KMPURL,
        currentTopId: String?,
    ) {
        lock.withLock {
            val currentJob = pollingJob.value
            val currentUrl = storyURL.value
            if (currentJob?.isActive == true && currentUrl == url) {
                return
            }

            this.storyURL.value = url
            this.lastKnownTopId.value = currentTopId
            _hasUpdates.value = false

            // Cancel previous polling, but NOT the whole monitorScope
            pollingJob.value?.cancel()

            pollingJob.value =
                monitorScope.launch {
                    while (isActive) {
                        delay(60_000L) // 60 seconds
                        checkForNewComments()
                    }
                }
        }
    }

    // Use this externally (from the Store/ViewModel) to reset everything
    fun stop() {
        lock.withLock {
            pollingJob.value?.cancel()
            pollingJob.value = null
            storyURL.value = null
            lastKnownTopId.value = null
            _hasUpdates.value = false
        }
    }

    private suspend fun checkForNewComments() {
        val url = storyURL.value ?: return
        val latestId = repo.checkForLatestId(url)
        val expectedTopId = lastKnownTopId.value

        if (latestId != null && latestId != expectedTopId) {
            lock.withLock {
                _hasUpdates.value = true
                pollingJob.value?.cancel()
                pollingJob.value = null // 🚩 Stop the network loop, but don't touch the flag
            }
        }
    }
}
