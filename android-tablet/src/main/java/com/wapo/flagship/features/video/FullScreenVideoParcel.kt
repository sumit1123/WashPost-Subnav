// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.video

import android.content.Context
import android.content.Intent
import com.wapo.flagship.features.posttv.model.Video

/**
 * Class to handle and build intent data for [FullScreenVideoActivity] class
 */
class FullScreenVideoParcel(
    private val intent: Intent,
) {
    fun getVideo(): Video? = intent.getParcelableExtra(VIDEO)

    fun getPlayerName(): String? = intent.getStringExtra(PLAYER_NAME)

    fun getLaunchMode(): FullScreenVideoActivity.LaunchMode? {
        val name = intent.getStringExtra(LAUNCH_MODE)
        return name?.let {
            FullScreenVideoActivity.LaunchMode.values().firstOrNull { it.name == name }
        }
    }

    class Builder {
        private var video: Video? = null
        private var playerName: String? = null
        private var launchMode: FullScreenVideoActivity.LaunchMode? = null

        fun setVideo(video: Video?): Builder {
            this.video = video
            return this
        }

        fun setPlayerName(playerName: String?): Builder {
            this.playerName = playerName
            return this
        }

        fun setMode(mode: FullScreenVideoActivity.LaunchMode?): Builder {
            this.launchMode = mode
            return this
        }

        fun buildIntent(source: Context?): Intent =
            Intent(source, FullScreenVideoActivity::class.java).also {
                it.putExtra(VIDEO, video)
                it.putExtra(PLAYER_NAME, playerName)
                it.putExtra(LAUNCH_MODE, launchMode?.name)
            }
    }

    companion object {
        const val VIDEO = "VIDEO"
        const val PLAYER_NAME = "PLAYER_NAME"
        const val LAUNCH_MODE = "MODE"
    }
}
