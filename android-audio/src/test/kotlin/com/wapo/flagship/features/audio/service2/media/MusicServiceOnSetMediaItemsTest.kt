package com.wapo.flagship.features.audio.service2.media

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.legacy.MediaDescriptionCompat
import androidx.media3.session.legacy.MediaSessionCompat
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.audio.service2.media.library.BrowseTree
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

/** Regression test for the duplicate playlist mutation that corrupts Media3 PlayerInfo. */
@RunWith(RobolectricTestRunner::class)
class MusicServiceOnSetMediaItemsTest {

    @Test
    fun `onSetMediaItems does not create an invalid Media3 PlayerInfo`() {
        val service = Robolectric.buildService(MusicService::class.java).get()
        val player = mockk<Player>(relaxed = true) {
            every { setMediaItems(any<List<MediaItem>>()) } answers {
                // This is the invariant that fails in Media3's PlayerInfo.Builder when the old
                // callback mutates the player before Media3 applies its returned playlist.
                buildInvalidMedia3PlayerInfo()
            }
        }
        val audioManager = mockk<ClassicAudioManager2>(relaxed = true)
        val controller = mockk<MediaSession.ControllerInfo>(relaxed = true)
        val mediaSession = mockk<MediaSession>(relaxed = true)

        every { controller.packageName } returns "com.google.android.projection.gearhead"
        every { audioManager.musicServiceConnection.playWhenReady.value } returns false
        service.audioManager = audioManager
        val mediaItem = MediaItem.Builder().setMediaId("item-1").build()
        val browseTree = mockk<BrowseTree>(relaxed = true)
        every { browseTree.getItem("item-1") } returns mediaItem
        setPrivateField(service, "browseTree", browseTree)
        setCurrentPlayer(service, player)

        val callback = newSessionCallback(service)
        val onSetMediaItems = callback.javaClass.getDeclaredMethod(
            "onSetMediaItems",
            MediaSession::class.java,
            MediaSession.ControllerInfo::class.java,
            MutableList::class.java,
            Int::class.javaPrimitiveType,
            Long::class.javaPrimitiveType
        ).apply { isAccessible = true }

        onSetMediaItems.invoke(
            callback,
            mediaSession,
            controller,
            mutableListOf(mediaItem),
            0,
            C.TIME_UNSET
        )
    }

    /** Throws from the same Media3 assertion shown in the production crash. */
    private fun buildInvalidMedia3PlayerInfo() {
        val queueTimelineClass = Class.forName("androidx.media3.session.QueueTimeline")
        val queueItem = MediaSessionCompat.QueueItem(
            MediaDescriptionCompat.Builder().setMediaId("item-1").build(),
            1L
        )
        val timeline = queueTimelineClass.getDeclaredMethod("create", List::class.java).apply {
            isAccessible = true
        }.invoke(null, listOf(queueItem))

        val stalePositionInfo = Player.PositionInfo(
            /* windowUid= */ null,
            /* mediaItemIndex= */ 1,
            /* mediaItem= */ null,
            /* periodUid= */ null,
            /* periodIndex= */ 0,
            /* positionMs= */ 0L,
            /* contentPositionMs= */ 0L,
            /* adGroupIndex= */ C.INDEX_UNSET,
            /* adIndexInAdGroup= */ C.INDEX_UNSET
        )
        val sessionPositionInfoClass = Class.forName("androidx.media3.session.SessionPositionInfo")
        val sessionPositionInfo = sessionPositionInfoClass.declaredConstructors.single().apply {
            isAccessible = true
        }.newInstance(
            stalePositionInfo,
            false,
            C.TIME_UNSET,
            C.TIME_UNSET,
            0L,
            0,
            0L,
            C.TIME_UNSET,
            C.TIME_UNSET,
            C.TIME_UNSET
        )

        val playerInfoClass = Class.forName("androidx.media3.session.PlayerInfo")
        val defaultPlayerInfo = playerInfoClass.getDeclaredField("DEFAULT").apply {
            isAccessible = true
        }.get(null)
        val builder = playerInfoClass.declaredClasses
            .single { it.simpleName == "Builder" }
            .getDeclaredConstructor(playerInfoClass).apply { isAccessible = true }
            .newInstance(defaultPlayerInfo)
        builder.javaClass.getDeclaredMethod(
            "setTimeline",
            Class.forName("androidx.media3.common.Timeline")
        ).apply { isAccessible = true }.invoke(builder, timeline)
        builder.javaClass.getDeclaredMethod("setSessionPositionInfo", sessionPositionInfoClass)
            .apply { isAccessible = true }.invoke(builder, sessionPositionInfo)
        builder.javaClass.getDeclaredMethod("build").apply { isAccessible = true }.invoke(builder)
    }

    private fun newSessionCallback(service: MusicService): Any {
        val callbackClass = service.javaClass.declaredClasses
            .single { it.simpleName == "SessionCallback" }
        return callbackClass.declaredConstructors.single().apply {
            isAccessible = true
        }.newInstance(service)
    }

    private fun setCurrentPlayer(service: MusicService, player: Player) {
        setPrivateField(service, "currentPlayer", player)
    }

    private fun setPrivateField(service: MusicService, name: String, value: Any) {
        service.javaClass.getDeclaredField(name).apply {
            isAccessible = true
            set(service, value)
        }
    }
}
