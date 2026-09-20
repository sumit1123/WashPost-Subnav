package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.interfaces.ExternalEventsCoordinator
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.articles2.models.deserialized.InlinePlayer
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemHumanAudioBinding
import com.washingtonpost.android.follow.viewmodel.FollowViewModel

class HumanAudioViewHolder(
    private val binding: ItemHumanAudioBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
    private val externalEventsCoordinator: ExternalEventsCoordinator,
    private val followViewModel: FollowViewModel,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Audio>(
        binding.root,
    ) {
    var inlinePlayer: InlinePlayer? = null
    private var mediaConfig: AudioMediaConfig? = null

    val observer: Observer<NowPlayingAudioItem?> =
        Observer { nowPlayingAudioItem ->
            updateUI(nowPlayingAudioItem)
        }

    override fun bind(
        item: Audio,
        position: Int,
    ) {
        mediaConfig = item.toAudioMediaConfig()
        updateUI(externalEventsCoordinator.provideNowPlayingAudioItemLiveData().value)
        externalEventsCoordinator.provideNowPlayingAudioItemLiveData().observeForever(observer)
        itemView.setOnClickListener {
            articlesInteractionHelper.onEventFired(ArticleInteractionEvent.AudioItemClicked(item.mediaId!!))
        }
        inlinePlayer = item.inlinePlayer
        if (inlinePlayer?.prefixImage != null) {
            binding.image.apply {
                visibility = View.VISIBLE
                followViewModel.followManager.followProvider
                    .getAuthorImageRequestUrl(
                        inlinePlayer?.prefixImage?.imageUrl,
                    )?.apply {
                        setImageUrl(
                            this,
                            followViewModel.followManager.followProvider.animatedImageLoader,
                        )
                    }
                setPlaceholder(com.washingtonpost.android.follow.R.drawable.author_placeholder)
                setErrorDrawable(com.washingtonpost.android.follow.R.drawable.author_placeholder)
            }
            binding.narration.setCompoundDrawables(null, null, null, null)
        } else {
            binding.image.visibility = View.GONE
            binding.narration.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(
                    binding.root.context,
                    R.drawable.narrated_audio_icon,
                ),
                null,
                null,
                null,
            )
        }
        binding.narration.text =
            item.inlinePlayer?.label ?: binding.root.context.getString(R.string.narrated_audio)
        item.duration?.let {
            binding.duration.text =
                AudioViewUtils.getDurationText(item.duration / 1000, binding.root.context)
        }
        super.bind(item, position)
    }

    override fun unbind() {
        externalEventsCoordinator.provideNowPlayingAudioItemLiveData().removeObserver(observer)
    }

    private fun readyState() {
        binding.listen.setCompoundDrawablesWithIntrinsicBounds(
            AppCompatResources.getDrawable(
                binding.root.context,
                R.drawable.article_audio_play,
            ),
            null,
            null,
            null,
        )
        binding.listen.text =
            inlinePlayer?.listen ?: binding.root.context.getString(R.string.listen)
        binding.duration.visibility = View.VISIBLE
    }

    private fun updateUI(nowPlayingAudioItem: NowPlayingAudioItem?) {
        val mediaConfig = nowPlayingAudioItem?.audioMediaConfig
        if (mediaConfig == null || mediaConfig.id.isEmpty() || mediaConfig.id != this.mediaConfig?.id) {
            readyState()
            return
        }
        when (nowPlayingAudioItem.audioPlaybackState) {
            is AudioPlaybackState.Playing -> {
                binding.listen.setCompoundDrawablesWithIntrinsicBounds(
                    AppCompatResources.getDrawable(
                        binding.root.context,
                        R.drawable.ic_polly_playing,
                    ),
                    null,
                    null,
                    null,
                )
                binding.listen.text = binding.root.context.getString(R.string.now_playing)
                binding.duration.visibility = View.GONE
            }
            AudioPlaybackState.Paused -> {
                binding.listen.setCompoundDrawablesWithIntrinsicBounds(
                    AppCompatResources.getDrawable(
                        binding.root.context,
                        R.drawable.article_audio_play,
                    ),
                    null,
                    null,
                    null,
                )
                binding.listen.text = binding.root.context.getString(R.string.now_playing)
                binding.duration.visibility = View.GONE
            }
            AudioPlaybackState.Buffering,
            AudioPlaybackState.JSONSourceInitializing,
            AudioPlaybackState.JSONSourceInitialized,
            -> {
                binding.listen.setCompoundDrawablesWithIntrinsicBounds(
                    AppCompatResources.getDrawable(
                        binding.root.context,
                        R.drawable.article_audio_play,
                    ),
                    null,
                    null,
                    null,
                )
                binding.listen.text = binding.root.context.getString(R.string.loading_polly)
                binding.duration.visibility = View.GONE
            }
            else -> readyState()
        }
    }
}
