// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.video

import android.content.Context
import android.content.Intent
import com.wapo.flagship.features.posttv.model.Video
import com.washingtonpost.android.config.domain.models.config.VerticalVideosConfig

/**
 * Class to handle and build intent data for [VerticalVideosActivity] class
 */
class VerticalVideosParcel(
    private val intent: Intent,
) {
    private var videos: List<Video> =
        intent.getParcelableArrayListExtra<Video>(VERTICAL_VIDEOS_LIST).orEmpty()

    fun getVideos(): List<Video> = videos

    fun getPosition(): Int = intent.getIntExtra(VERTICAL_VIDEOS_POSITION, -1)
    fun getOffset(): Int = intent.getIntExtra(VERTICAL_VIDEOS_OFFSET, -1)

    fun getConfig(): VerticalVideosConfig? = intent.getParcelableExtra(VERTICAL_VIDEOS_CONFIG)

    fun getSourceScreen(): String? = intent.getStringExtra(WATCH_VIDEOS_SCREEN)

    fun getTabName(): String? = intent.getStringExtra(WATCH_VIDEO_TAB_NAME)

    class Builder {
        private var postTvVideos: List<Video>? = null
        private var position: Int = -1
        private var offset: Int = -1
        private var verticalVideosConfig: VerticalVideosConfig? = null
        private var sourceScreen: String = ""
        private var tabName: String? = null

        fun setPostTvVideos(videos: List<Video>): Builder {
            this.postTvVideos = videos
            return this
        }

        fun setPosition(pos: Int): Builder {
            this.position = pos
            return this
        }

        fun setOffset(offset: Int): Builder {
            this.offset = offset
            return this
        }

        fun setVerticalVideosConfig(config: VerticalVideosConfig?): Builder {
            this.verticalVideosConfig = config
            return this
        }

        fun setSourceScreen(sourceScreen: String): Builder {
            this.sourceScreen = sourceScreen
            return this
        }

        fun setTabName(tabName: String): Builder {
            this.tabName = tabName
            return this
        }

        fun buildIntent(source: Context?): Intent =
            Intent(source, VerticalVideosActivity::class.java).also {
                it.putParcelableArrayListExtra(VERTICAL_VIDEOS_LIST, ArrayList(postTvVideos))
                it.putExtra(VERTICAL_VIDEOS_POSITION, position)
                it.putExtra(VERTICAL_VIDEOS_OFFSET, offset)
                it.putExtra(VERTICAL_VIDEOS_CONFIG, verticalVideosConfig)
                it.putExtra(WATCH_VIDEOS_SCREEN, sourceScreen)
                it.putExtra(WATCH_VIDEO_TAB_NAME, tabName)
            }
    }

    companion object {
        const val VERTICAL_VIDEOS_LIST = "VERTICAL_VIDEOS_LIST"
        const val VERTICAL_VIDEOS_POSITION = "VERTICAL_VIDEOS_POSITION"
        const val VERTICAL_VIDEOS_OFFSET = "VERTICAL_VIDEOS_OFFSET"
        const val VERTICAL_VIDEOS_CONFIG = "VERTICAL_VIDEOS_CONFIG"
        const val WATCH_VIDEOS_SCREEN = "WATCH_VIDEOS_SCREEN"
        const val WATCH_VIDEO_TAB_NAME = "WATCH_VIDEO_TAB_NAME"
    }
}
