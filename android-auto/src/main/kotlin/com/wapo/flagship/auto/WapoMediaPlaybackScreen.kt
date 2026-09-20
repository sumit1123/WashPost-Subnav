package com.wapo.flagship.auto

import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.media.model.MediaPlaybackTemplate
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat

@OptIn(ExperimentalCarApi::class)
internal class WapoMediaPlaybackScreen(
    carContext: CarContext,
    private val mediaCoordinator: CarMediaCoordinator,
) : Screen(carContext) {
    private val queueIcon =
        CarIcon
            .Builder(
                IconCompat.createWithResource(
                    carContext,
                    com.wapo.flagship.features.audio.R.drawable.ic_playlist_active,
                ),
            ).setTint(CarColor.DEFAULT)
            .build()

    override fun onGetTemplate(): Template =
        MediaPlaybackTemplate
            .Builder()
            .setHeader(
                Header
                    .Builder()
                    .setTitle(carContext.getString(R.string.android_auto_now_playing))
                    .addEndHeaderAction(
                        Action
                            .Builder()
                            .setTitle(carContext.getString(R.string.android_auto_queue))
                            .setIcon(queueIcon)
                            .setOnClickListener(::showQueue)
                            .build(),
                    )
                    .build(),
            ).build()

    private fun showQueue() {
        carContext
            .getCarService(ScreenManager::class.java)
            .push(WapoPlaybackQueueScreen(carContext, mediaCoordinator))
    }
}
