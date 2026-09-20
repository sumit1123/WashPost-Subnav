package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ExternalEventsCoordinator
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.wapo.flagship.util.AudioUtil
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemAudioBinding

class AudioViewHolder(
    private val binding: ItemAudioBinding,
    private val externalEventsCoordinator: ExternalEventsCoordinator,
    private val lifecycleOwner: LifecycleOwner,
    private val onAudioClicked: (Audio) -> Unit,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Audio>(
        binding.root,
    ) {
    private var mediaConfig: AudioMediaConfig? = null

    val observer: Observer<NowPlayingAudioItem?> =
        Observer { nowPlayingAudioItem ->
            updateUI(nowPlayingAudioItem)
        }

    private var isObserving = false

    override fun bind(
        item: Audio,
        position: Int,
    ) {
        val updatedItem = updateDuration(item)
        mediaConfig = updatedItem.toAudioMediaConfig()
        updateUI(externalEventsCoordinator.provideNowPlayingAudioItemLiveData().value)
        if (!isObserving) {
            externalEventsCoordinator
                .provideNowPlayingAudioItemLiveData()
                .observe(lifecycleOwner, observer)
            isObserving = true
        }
        itemView.setOnClickListener {
            onAudioClicked(item)
        }

        super.bind(item, position)
    }

    override fun unbind() {
        if (isObserving) {
            externalEventsCoordinator
                .provideNowPlayingAudioItemLiveData()
                .removeObserver(observer)
            isObserving = false
        }
        mediaConfig = null
    }

    private fun readyState() {
        // no-operation
        binding.audioButton.visibility = View.VISIBLE
        binding.statusProgressBar.visibility = View.GONE
        binding.audioButton.background =
            AppCompatResources.getDrawable(
                itemView.context,
                com.wapo.view.R.drawable.audio_pill_default,
            )
        binding.statusIcon.setImageResource(com.wapo.view.R.drawable.play_icon_btn)
        binding.itemDuration.setTextColor(
            ContextCompat.getColor(itemView.context, com.wapo.view.R.color.headline_text_color),
        )
        // should use inlinePlayer.label here?
        binding.itemDuration.text =
            AudioViewUtils.getDurationText(mediaConfig?.duration?.div(1000), itemView.context)
    }

    private fun updateDuration(item: Audio): Audio{
        val duration = AudioUtil.calculateTotalDuration(item)
        return item.copy(duration = duration)
    }

    private fun updateUI(nowPlayingAudioItem: NowPlayingAudioItem?) {
        val mediaConfig = nowPlayingAudioItem?.audioMediaConfig
        if (mediaConfig == null || mediaConfig.id.isEmpty() || mediaConfig.id != this.mediaConfig?.id) {
            readyState()
            return
        }
        when (nowPlayingAudioItem.audioPlaybackState) {
            is AudioPlaybackState.Playing -> {
                binding.statusProgressBar.visibility = View.GONE
                binding.statusIcon.visibility = View.VISIBLE
                binding.statusIcon.setImageResource(com.wapo.view.R.drawable.pause_icon_btn)
                binding.itemDuration.setTextColor(
                    ContextCompat.getColor(itemView.context, com.wapo.flagship.features.audio.R.color.pill_duration_text),
                )
                binding.audioButton.background =
                    ContextCompat.getDrawable(
                        itemView.context,
                        com.wapo.view.R.drawable.audio_pill_play,
                    )
            }
            AudioPlaybackState.Paused -> {
                binding.audioButton.background =
                    AppCompatResources.getDrawable(
                        itemView.context,
                        com.wapo.view.R.drawable.audio_pill_default,
                    )
                binding.statusIcon.setImageResource(com.wapo.view.R.drawable.play_icon_btn)
                binding.itemDuration.setTextColor(
                    ContextCompat.getColor(itemView.context, com.wapo.view.R.color.headline_text_color),
                )
            }
            AudioPlaybackState.Buffering,
            AudioPlaybackState.JSONSourceInitializing,
            AudioPlaybackState.JSONSourceInitialized,
            -> {
                binding.statusProgressBar.visibility = View.VISIBLE
                binding.statusIcon.visibility = View.INVISIBLE
            }
            else -> readyState()
        }
    }
}
