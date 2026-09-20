package com.wapo.flagship.features.grid.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.setVisible
import com.wapo.android.commons.util.truncatedString
import com.wapo.flagship.features.audio.AudioMediaActivity
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.events.ActionButtonEvent
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.grid.model.HomepageStory
import com.wapo.flagship.features.grid.model.Signature
import com.wapo.flagship.json.TrackingInfo
import com.wapo.view.RippleHelper
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.sections.databinding.ViewActionButtonsBinding
import java.lang.ref.WeakReference

class SignatureActionsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var binding: ViewActionButtonsBinding

    var onClick: WeakReference<((ActionButtonEvent) -> Unit)?>? = null
    private var nowPlayingObserver: Observer<NowPlayingAudioItem?>? = null
    private var articleSaved: LiveData<Boolean>? = null
    private var articleSavedObserver: Observer<Boolean>? = null
    private var audioMediaConfig: AudioMediaConfig? = null
    private val activityViewModel = context.findActivityOfType<AudioMediaActivity>()?.getAudioMediaActivityViewModel()
    val signature
        get() = binding.signature
    private val gridActivity get() = findActivityOfType<GridActivity>()

    init {
        val inflater = LayoutInflater.from(getContext())
        binding = ViewActionButtonsBinding.inflate(inflater, this, true)
        RippleHelper.addRippleEffectToView(binding.listen)
        RippleHelper.addRippleEffectToView(binding.share)
        RippleHelper.addRippleEffectToView(binding.menu)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        observeNowPlayingMediaItemObserver()
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        nowPlayingObserver?.let {
            activityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        articleSavedObserver?.let {
            articleSaved?.removeObserver(it)
        }
        nowPlayingObserver = null
        articleSavedObserver = null
    }
    
    fun initActionButtons(homepageStory: HomepageStory) {
        if (homepageStory.actions?.audioArticle == null) {
            binding.listen.visibility = GONE
        } else {
            audioMediaConfig = gridActivity?.getGridEnvironment()
                ?.generateAudioMediaConfig(homepageStory.actions.audioArticle, isFlexFeature = false, isActionButton = true)
            binding.listen.visibility = VISIBLE
            binding.listen.setOnClickListener {
                audioMediaConfig?.let { config ->
                    onClick?.get()?.invoke(ActionButtonEvent.Listen(config))
                }
            }
        }

        val environment = gridActivity?.getGridEnvironment()
        if (environment?.isSaveEnabled() == true) {
            homepageStory.link?.let { storyLink ->
                articleSaved = context.findActivityOfType<SavedVerifierActivity>()?.isArticleSaved(url = storyLink.url)
                observeArticleSaved(homepageStory)
            }
        } else {
            binding.save.setVisible(false)
        }

        binding.share.setOnClickListener {
            onClick?.get()?.invoke(
                ActionButtonEvent.Share(
                    homepageStory.headline?.text,
                    homepageStory.signature?.byLine,
                    homepageStory.link?.url
                )
            )
        }

        binding.menu.setOnClickListener {
            onClick?.get()?.invoke(
                ActionButtonEvent.Menu(
                    homepageStory.toMenuItem(EllipsisMenu.ActionButton)
                )
            )
        }

        val commentsAction = homepageStory.actions?.comments
        binding.apply {
            comments.setVisible(commentsAction != null)
            if (commentsAction != null) {
                commentsCount.text = commentsAction.count.truncatedString().orEmpty()
                comments.contentDescription = "Comments (${commentsAction.count.truncatedString().orEmpty()})"
                comments.setOnClickListener {
                    val commentsStoryId = homepageStory.contentId
                        ?: homepageStory.actions.audioArticle?.tracking?.arcId
                        ?: ""
                    val trackingInfo = TrackingInfo().apply {
                        arcId = commentsStoryId
                        contentURL = homepageStory.link?.url ?: ""
                        title = homepageStory.headline?.text
                        pageName ="front - top stories-${homepageStory.actions.audioArticle?.tracking?.pageName}"
                    }
                    onClick?.get()?.invoke(
                        ActionButtonEvent.Comments(
                            url = homepageStory.link?.url ?: "",
                            storyId = commentsStoryId,
                            storyTitle = homepageStory.headline?.text ?: "",
                            trackingInfo = trackingInfo
                        )
                    )
                }
            }
        }
    }

    fun showActionButtons(show: Boolean) {
        binding.actionButtons.setVisible(show)
    }

    fun setSignature(signature: Signature, isGrid: Boolean) {
        binding.signature.setSignature(signature, isGrid)
    }

    fun setTextGravity(gravity: Int) {
        binding.signature.setTextGravity(gravity)
    }
    fun updateBylineColor(color: Int) {
        binding.signature.updateBylineColor(color)
    }

    private fun HomepageStory.toMenuItem(menuType: EllipsisMenu): EllipsisActionItem {
        val isAudioArticle = this.actions?.audioArticle != null
        val audioMediaConfig = this.actions?.audioArticle?.let {
            gridActivity
                ?.getGridEnvironment()
                ?.generateAudioMediaConfig(it, isFlexFeature = false, isActionButton = true)
        }
        return EllipsisActionItem(
            menuType = menuType,
            url = this.link?.url ?: "",
            byline = this.signature?.byLine ?: "",
            headline = this.headline?.text ?: "",
            isAudioArticle = isAudioArticle,
            imageUrl = this.media?.url ?: "",
            audioMediaConfig = audioMediaConfig
        )
    }

    private fun observeNowPlayingMediaItemObserver() {
        nowPlayingObserver?.let {
            activityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver = Observer<NowPlayingAudioItem?> { nowPlayingAudioItem ->
            if (audioMediaConfig == null) return@Observer
            nowPlayingAudioItem ?: return@Observer
            if ((nowPlayingAudioItem.audioMediaConfig?.id == audioMediaConfig?.id)) {
                when (nowPlayingAudioItem.audioPlaybackState) {
                    AudioPlaybackState.Connecting,
                    AudioPlaybackState.Buffering,
                    AudioPlaybackState.JSONSourceInitializing,
                    AudioPlaybackState.JSONSourceInitialized -> {
                        binding.loadingSpinner.visibility = VISIBLE
                        binding.listen.visibility = GONE
                    }
                    is AudioPlaybackState.Playing -> {
                        binding.listen.visibility = VISIBLE
                        binding.listen.setImageResource(R.drawable.ic_ab_headphones_filled)
                        binding.loadingSpinner.visibility = GONE
                    }
                    else -> {
                        binding.loadingSpinner.visibility = GONE
                        binding.listen.visibility = VISIBLE
                        binding.listen.setImageResource(R.drawable.ic_ab_headphones)
                    }
                }
            } else {
                binding.loadingSpinner.visibility = GONE
                binding.listen.visibility = VISIBLE
                binding.listen.setImageResource(R.drawable.ic_ab_headphones)
            }
        }.also {
            activityViewModel?.nowPlayingAudioItem?.observeForever(it)
        }
    }

    private fun observeArticleSaved(homepageStory: HomepageStory) {
        articleSavedObserver?.let {
            articleSaved?.removeObserver(it)
        }
        articleSavedObserver = Observer<Boolean> { isSaved ->
            with(binding.save) {
                if (isSaved) {
                    setImageResource(R.drawable.ic_ab_article_save_filled)
                    setOnClickListener {
                        onClick?.get()?.invoke(
                            ActionButtonEvent.Remove(
                                homepageStory.toMenuItem(EllipsisMenu.ActionButton)
                            )
                        )
                    }
                } else {
                    setImageResource(R.drawable.ic_ab_article_save)
                    setOnClickListener {
                        val environment = gridActivity?.getGridEnvironment()
                        if (environment?.isLoggedInUser() == true) {
                            onClick?.get()?.invoke(
                                ActionButtonEvent.Save(
                                    homepageStory.toMenuItem(EllipsisMenu.ActionButton)
                                )
                            )
                        } else {
                            // if user is neither logged in nor subscribed, show Save Regwall
                            environment?.showSaveRegwall(this.context)
                        }
                    }
                }
                setVisible(true)
            }
        }.also {
            articleSaved?.observeForever(it)
        }
    }

}

interface SavedVerifierActivity {
    fun isArticleSaved(url: String): LiveData<Boolean>
}