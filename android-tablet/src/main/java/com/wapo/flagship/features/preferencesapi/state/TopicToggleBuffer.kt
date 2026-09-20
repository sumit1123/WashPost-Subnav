package com.wapo.flagship.features.preferencesapi.state

import com.wapo.flagship.features.preferencesapi.repo.TopicNotificationsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TopicToggleBuffer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private const val DEBOUNCE_MS = 1200L

    fun onLocalTopicChanged() {
        job?.cancel()
        job = scope.launch {
            delay(DEBOUNCE_MS)
            TopicNotificationsRepo.getInstance().syncTopicsWithPreferencesApi()
        }
    }
}