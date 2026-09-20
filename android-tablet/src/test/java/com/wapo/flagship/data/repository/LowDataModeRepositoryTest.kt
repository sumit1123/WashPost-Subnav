package com.wapo.flagship.data.repository

import com.wapo.flagship.features.lowdata.LowDataModeDataStore
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl
import com.wapo.flagship.features.lowdata.LowDataModeRepo
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LowDataModeRepositoryTest {
    private lateinit var lowDataModeRepo: LowDataModeRepo

    private fun createLowDataModeRepo(
        lowDataModeDataStore: LowDataModeDataStore = mockk(relaxed = true)
    ): LowDataModeRepo {
        return LowDataModeNotificationImpl(lowDataModeDataStore)
    }

    @Test
    fun `dismissNotification updates state to Snooze`() = runTest {
        // GIVEN
        lowDataModeRepo = createLowDataModeRepo()

        // WHEN
        val result = lowDataModeRepo.dismissNotification()

        // THEN
        assert(result.notificationState is LowDataModeNotificationImpl.LowDataModeNotificationState.Snooze)
    }

    @Test
    fun `setLowDataModeNotificationState updates state to Dialog`() = runTest {
        // GIVEN
        lowDataModeRepo = createLowDataModeRepo()
        val newState = LowDataModeNotificationImpl.LowDataModeNotificationState.Dialog

        // WHEN
        val result = lowDataModeRepo.setLowDataModeNotificationState(newState)

        // THEN
        assert(result.notificationState is LowDataModeNotificationImpl.LowDataModeNotificationState.Dialog)
    }
}