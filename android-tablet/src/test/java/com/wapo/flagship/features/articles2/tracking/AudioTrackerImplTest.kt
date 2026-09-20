package com.wapo.flagship.features.articles2.tracking

import androidx.media3.common.Player
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class AudioTrackerImplTest {
    val audioTrackerImpl = AudioTrackerImpl()

    @Mock
    lateinit var player: Player

    @Before
    fun setup() {
    }

    @Test
    fun startTrackingProgress() {
    }
}
