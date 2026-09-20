package com.wapo.flagship.features.articles2.viewholders

import android.text.SpannableString
import android.text.format.DateUtils
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.squareup.picasso.Picasso
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.podcast.Podcast
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.features.audio.*
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.washingtonpost.android.databinding.ItemPodcastBinding

class PodcastViewHolder(
    private val binding: ItemPodcastBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<Podcast>(binding.root, null) {
    private var time: Long? = null
    private var nowPlayingObserver: Observer<NowPlayingAudioItem?>? = null
    private val activityViewModel =
        binding.root.findActivityOfType<AudioMediaActivity>()?.getAudioMediaActivityViewModel()
    private val audioManager =
        binding.root.findActivityOfType<AudioMediaActivity>()?.getAudioManager()

    override fun onBindItem(
        item: Podcast,
        position: Int,
    ) {
        val context = itemView.context

        val podcastName = item.seriesName
        val podcastEpisode = item.episodeName
        time = item.duration

        val name = SpannableString("$podcastName Podcast")
        name.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0,
            podcastName.length,
            0,
        )
        binding.apply {
            seriesTitle.text = name
            episodeTitle.text = podcastEpisode
            setStatus(context.getString(R.string.listen), time)
            bindMedia(item)
        }
    }

    override fun setPlaceHolderData(item: Podcast): PlaceHolderData? = null

    override fun onLowDataModeEnable(item: Podcast) {
        binding.image.isVisible = false
    }

    override fun onLowDataModeDisable(item: Podcast) {
        var imageUrl = item.seriesImageUrl

        binding.image.isVisible = true
        audioManager?.audioProvider?.getThumbnailImageRequestURL(imageUrl)?.apply {
            imageUrl = this
        }
        Picasso
            .get()
            .load(imageUrl)
            .noPlaceholder()
            .error(R.drawable.wp_placeholder_medium)
            .into(binding.image)
    }

    private fun setStatus(
        text: String,
        timeInSeconds: Long? = null,
    ) {
        val content =
            SpannableString(
                if (timeInSeconds == null) {
                    text
                } else {
                    "$text - ${DateUtils.formatElapsedTime(timeInSeconds)}"
                },
            )
        content.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0,
            text.length,
            0,
        )
        binding.status.text = content
    }

    private fun bindMedia(item: Podcast) {
        binding.exoPlay.visibility = View.VISIBLE
        binding.exoPause.visibility = View.GONE
        binding.loadingSpinner.visibility = View.GONE
        itemView.setOnClickListener { _ ->
            val audioMediaConfig = item.toAudioMediaConfig()
            activityViewModel?.playMedia(audioMediaConfig)
        }
        nowPlayingObserver?.let {
            activityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver =
            Observer<NowPlayingAudioItem?> { nowPlayingAudioItem ->
                nowPlayingAudioItem ?: return@Observer
                if (nowPlayingAudioItem.audioPlaybackState == AudioPlaybackState.JSONSourceInitializing ||
                    nowPlayingAudioItem.audioPlaybackState == AudioPlaybackState.JSONSourceInitialized
                ) {
                    setStatus("Buffering")
                    binding.exoPlay.visibility = View.GONE
                    binding.exoPause.visibility = View.GONE
                    binding.loadingSpinner.visibility = View.VISIBLE
                    return@Observer
                }
                val mediaItem = nowPlayingAudioItem.mediaItemData ?: return@Observer
                if (mediaItem.mediaId != null && mediaItem.mediaId == item.mediaId) {
                    when (nowPlayingAudioItem.audioPlaybackState) {
                        is AudioPlaybackState.Playing -> {
                            setStatus("Now Playing")
                            binding.loadingSpinner.visibility = View.GONE
                            binding.exoPlay.visibility = View.GONE
                            binding.exoPause.visibility = View.VISIBLE
                        }
                        AudioPlaybackState.Connecting,
                        AudioPlaybackState.Buffering,
                        -> {
                            setStatus("Buffering")
                            binding.exoPlay.visibility = View.GONE
                            binding.exoPause.visibility = View.GONE
                            binding.loadingSpinner.visibility = View.VISIBLE
                        }
                        else -> {
                            setStatus(itemView.context.getString(R.string.listen), time)
                            binding.loadingSpinner.visibility = View.GONE
                            binding.exoPause.visibility = View.GONE
                            binding.exoPlay.visibility = View.VISIBLE
                        }
                    }
                }
            }.also {
                activityViewModel?.nowPlayingAudioItem?.observeForever(it)
            }
    }

    fun unbindMedia() {
        itemView.setOnClickListener(null)
        nowPlayingObserver?.let {
            activityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver = null
    }
}
