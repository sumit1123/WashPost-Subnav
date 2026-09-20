package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import androidx.lifecycle.Observer
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.interfaces.ExternalEventsCoordinator
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemStandaloneAudioBinding

class StandaloneAudioViewHolder(
    private val binding: ItemStandaloneAudioBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
    private val externalEventsCoordinator: ExternalEventsCoordinator,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Audio>(binding.root) {
    private var mediaConfig: AudioMediaConfig? = null

    val observer: Observer<NowPlayingAudioItem?> =
        Observer { nowPlayingAudioItem ->
            updateUI(nowPlayingAudioItem)
        }

    override fun bind(
        item: Audio,
        position: Int,
    ) {
        mediaConfig = item.toAudioMediaConfig(PlayerType.STANDALONE, null, null)
        binding.caption.text = item.title?.content
        binding.duration.text =
            AudioViewUtils.getDurationText(item.duration?.let { it / 1000 }, itemView.context)
        updateUI(externalEventsCoordinator.provideNowPlayingAudioItemLiveData().value)
        externalEventsCoordinator.provideNowPlayingAudioItemLiveData().observeForever(observer)
        itemView.setOnClickListener {
            articlesInteractionHelper.onEventFired(ArticleInteractionEvent.AudioItemClicked(item.mediaId!!))
        }

        super.bind(item, position)
    }

    override fun unbind() {
        externalEventsCoordinator.provideNowPlayingAudioItemLiveData().removeObserver(observer)
        mediaConfig = null
    }

    private fun playState() {
        binding.statusIcon.setImageResource(R.drawable.ic_standalone_audio_pause)
        binding.statusProgressBar.visibility = View.GONE
        binding.statusText.text = binding.root.context.getString(R.string.now_playing)
        binding.duration.visibility = View.GONE
    }

    private fun readyState() {
        binding.statusIcon.setImageResource(R.drawable.ic_standalone_audio_play)
        binding.statusProgressBar.visibility = View.GONE
        // should use inlinePlayer.label here?
        binding.statusText.text = binding.root.context.getString(R.string.listen)
        binding.duration.visibility =
            if (binding.duration.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.caption.visibility =
            if (binding.caption.text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadingState() {
        binding.statusProgressBar.visibility = View.VISIBLE
    }

    private fun updateUI(nowPlayingAudioItem: NowPlayingAudioItem?) {
        val mediaConfig = nowPlayingAudioItem?.audioMediaConfig
        if (mediaConfig == null || mediaConfig.id.isEmpty() || mediaConfig.id != this.mediaConfig?.id) {
            readyState()
            return
        }
        when (nowPlayingAudioItem.audioPlaybackState) {
            is AudioPlaybackState.Playing -> playState()
            AudioPlaybackState.Paused -> readyState()
            AudioPlaybackState.Buffering,
            AudioPlaybackState.JSONSourceInitializing,
            AudioPlaybackState.JSONSourceInitialized,
            -> loadingState()

            else -> readyState()
        }
    }
}
