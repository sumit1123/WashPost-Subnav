// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import androidx.lifecycle.Observer
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.podcast.Podcast
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemInlinePodcastBinding

class InlinePodcastViewHolder(
    private val binding: ItemInlinePodcastBinding,
    private val audioMediaActivityViewModel: AudioMediaActivityViewModel?,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Podcast>(binding.root) {
    private var isCurrentMediaActive: Boolean = false
    private var nowPlayingObserver: Observer<NowPlayingAudioItem?>? = null
    private var item: Podcast? = null

    override fun bind(
        item: Podcast,
        position: Int,
    ) {
        val context = itemView.context
        this.item = item

        binding.playerStatus.text = getTitle()
        binding.playerDuration.text = AudioViewUtils.getDurationText(item.duration, context)

        bindMedia(item)
    }

    private fun bindMedia(podcast: Podcast) {
        binding.playPause.setImageResource(com.wapo.view.R.drawable.ic_podcast_play)
        binding.loadingSpinner.visibility = View.GONE
        itemView.setOnClickListener { _ ->
            val audioMediaConfig = podcast.toAudioMediaConfig()
            audioMediaActivityViewModel?.playMedia(audioMediaConfig)
        }
        observeNowPlayingMediaItemObserver()
    }

    fun unbindMedia() {
        itemView.setOnClickListener(null)
        nowPlayingObserver?.let {
            audioMediaActivityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver = null
    }

    private fun observeNowPlayingMediaItemObserver() {
        nowPlayingObserver?.let {
            audioMediaActivityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver =
            Observer<NowPlayingAudioItem?> { nowPlayingAudioItem ->
                val mediaItem = nowPlayingAudioItem?.mediaItemData ?: return@Observer
                if (mediaItem.mediaId != null && mediaItem.mediaId == item?.mediaId) {
                    when (nowPlayingAudioItem.audioPlaybackState) {
                        is AudioPlaybackState.Playing -> {
                            binding.playerDuration.visibility = View.GONE
                            binding.playerStatus.text =
                                binding.root.context.getString(
                                    R.string.now_playing,
                                )
                            binding.loadingSpinner.visibility = View.GONE
                            binding.playPause.setImageResource(com.wapo.view.R.drawable.ic_podcast_pause)
                            isCurrentMediaActive = true
                        }
                        AudioPlaybackState.Connecting,
                        AudioPlaybackState.Buffering,
                        -> {
                            binding.playerStatus.text =
                                binding.root.context.getString(
                                    com.wapo.flagship.features.audio.R.string.buffering,
                                )
                            binding.playerDuration.visibility = View.GONE
                            binding.loadingSpinner.visibility = View.VISIBLE
                            isCurrentMediaActive = true
                        }
                        else -> {
                            resetUi()
                        }
                    }
                } else {
                    resetUi()
                }
            }.also {
                audioMediaActivityViewModel?.nowPlayingAudioItem?.observeForever(it)
            }
    }

    private fun resetUi() {
        binding.playPause.visibility = View.VISIBLE
        binding.playerDuration.visibility = View.VISIBLE
        binding.playerStatus.text = getTitle()
        binding.loadingSpinner.visibility = View.GONE
        binding.playPause.setImageResource(com.wapo.view.R.drawable.ic_podcast_play)
        isCurrentMediaActive = false
    }

    private fun getTitle(): String =
        item?.inlinePlayer?.listen
            ?: binding.root.resources.getString(R.string.listen_to_podcast)
}
