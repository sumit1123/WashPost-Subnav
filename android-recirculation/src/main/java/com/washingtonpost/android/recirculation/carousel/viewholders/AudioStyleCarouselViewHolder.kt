/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.wapo.android.commons.util.getUrlWithoutParameters
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
import com.washingtonpost.android.follow.ui.CircleImageView
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.adapter.CarouselRecyclerViewAdapter
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselAudioViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.EllipsisActionItem
import java.lang.ref.WeakReference

class AudioStyleCarouselViewHolder(
    itemView: View, val carouselProvider: CarouselProvider?,
    private var clickListener: OnCarouselClickedListener?,
    private val cardWidth: Int,
    private val cardHeight: Int,
    private var onEllipsisClick: ((EllipsisActionItem) -> Unit)?,
    private val urlList: List<String> = listOf(),
    private var setResizeCarouselView: (() -> Unit)?,
    private var resizeCarouselView: (() -> Unit)?,
    private var getNowPlayingAudioItem: (() -> NowPlayingAudioItem?)?,
) : CarouselViewHolder(itemView), CarouselRecyclerViewAdapter.AudioViewHolder {

    private var imageViewRef: WeakReference<ImageView?>? = null
    private var cardView: MaterialCardView? = null
    private lateinit var artView: ImageView
    private lateinit var overlayTextView: TextView
    private lateinit var artOverlay: LinearLayout
    private lateinit var prefixImage: CircleImageView
    private lateinit var headlineView: TextView
    private lateinit var dateView: TextView
    private lateinit var playerView: ViewGroup
    private lateinit var playerStatusIcon: ImageView
    private lateinit var playerStatusProgressBar: ProgressBar
    private lateinit var playerStatusText: TextView
    private lateinit var playerDurationText: TextView
    private lateinit var ellipsisMenu: ImageButton

    override fun bind(item: CarouselViewItem) {
        val viewItem: CarouselAudioViewItem = item as CarouselAudioViewItem
        cardView = itemView.findViewById(R.id.audio_item_card)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cardView?.elevation = 0f
            cardView?.strokeWidth = 0
        }

        artView = itemView.findViewById(R.id.item_art)
        artOverlay = itemView.findViewById(R.id.item_overlay)
        overlayTextView = itemView.findViewById(R.id.text_overlay)
        prefixImage = itemView.findViewById(R.id.prefix_media_overlay)
        imageViewRef = WeakReference(artView)
        cardView?.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        if (viewItem.imageUrl != null) {
            artView.visibility = View.VISIBLE
            carouselProvider?.makeImageRequest(viewItem.imageUrl, cardWidth, 0, this)
        } else {
            artView.visibility = View.GONE
        }
        if (item.overlayText != null) {
            artOverlay.visibility = View.VISIBLE
            overlayTextView.visibility = View.VISIBLE
            overlayTextView.text = item.overlayText
            if (item.overlayPrefixImageUrl != null) {
                prefixImage.visibility = View.VISIBLE
                prefixImage.apply {
                    setImageUrl(item.overlayPrefixImageUrl, carouselProvider?.getImageLoader())
                }
            } else {
                prefixImage.visibility = View.GONE
                overlayTextView.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.narrated_audio_white
                    ), null, null, null
                )
            }
        } else {
            artOverlay.visibility = View.GONE
            overlayTextView.visibility = View.GONE
            prefixImage.visibility = View.GONE
        }


        headlineView = itemView.findViewById(R.id.item_headline)
        dateView = itemView.findViewById(R.id.item_date)
        headlineView.text = item.headline
        dateView.text = item.displayDate
        playerView = itemView.findViewById(R.id.carousel_audio_item_player_status)
        playerStatusIcon = playerView.findViewById(R.id.status_icon)
        playerStatusText = playerView.findViewById(R.id.status_text)
        playerStatusProgressBar = playerView.findViewById(R.id.status_progress_bar)
        playerDurationText = playerView.findViewById(R.id.item_duration)
        playerDurationText.text =
            AudioViewUtils.getDurationText(viewItem.duration, itemView.context)

        itemView.findViewById<MaterialCardView>(R.id.audio_item_card).setOnClickListener {
            clickListener?.onCardClicked(viewItem.contentUrl, position)
        }

        ellipsisMenu = itemView.findViewById(R.id.ellipsis_menu)

        if (PersonalizedPodcastHelper.isPersonalizedPodcastItem( viewItem.carouselItemType)) {
            if (viewItem.carouselItemType != PLACEHOLDER) {
                dateView.visibility = View.VISIBLE
            } else {
                artOverlay.visibility = View.VISIBLE
                overlayTextView.visibility = View.VISIBLE
                overlayTextView.text = getString(itemView.context, R.string.new_podcast)
            }
            ellipsisMenu.visibility = View.GONE
        } else {
            dateView.visibility = View.GONE
            ellipsisMenu.visibility = View.VISIBLE
        }

        ellipsisMenu.setOnClickListener {
            onEllipsisClick?.let { it1 ->
                it1(
                    EllipsisActionItem(
                        url = viewItem.articleUrl,
                        imageUrl = viewItem.imageUrl ?: "",
                        byline = viewItem.byline,
                        headline = viewItem.headline,
                        articleList = urlList,
                        pageName = viewItem.pageName ?: "",
                        viewItem.articleLinkIsWebType,
                        viewItem.carouselItemType ?: ""
                    )
                )
            }
        }

        onNowPlayingAudioItem(item, getNowPlayingAudioItem?.invoke())
    }

    override fun onBitmapLoaded(bitmap: Bitmap) {
        if (imageViewRef?.get() != null) {
            val animation = AlphaAnimation(0f, 1f)
            animation.duration = 1500
            imageViewRef?.get()?.setImageBitmap(bitmap)
            imageViewRef?.get()?.startAnimation(animation)
            imageViewRef?.get()?.background = null
        }
    }

    override fun onBitmapError(bitmap: Bitmap?) {
        bitmap?.let { onBitmapLoaded(it) }
    }

    override fun onLowDataModeChange(isEnable: Boolean) {
        if (isEnable) {
            imageViewRef?.get()?.isVisible = false
            setResizeCarouselView?.invoke()
        } else {
            imageViewRef?.get()?.isVisible = true
            resizeCarouselView?.invoke()
        }
    }

    override fun onViewRecycled() {
        imageViewRef?.get()?.setImageDrawable(null)
        itemView.findViewById<MaterialCardView>(R.id.audio_item_card).setOnClickListener(null)
        ellipsisMenu.setOnClickListener(null)
    }

    override fun readyState() {
        val audioBtn = itemView.findViewById<ConstraintLayout>(R.id.audio_button)
        audioBtn.background = ContextCompat.getDrawable(itemView.context, com.wapo.view.R.drawable.audio_pill_default)
        playerStatusIcon.setImageResource(com.wapo.view.R.drawable.play_icon_btn)
        playerStatusIcon.visibility = View.VISIBLE
        playerDurationText.setTextColor(ContextCompat.getColor(itemView.context, com.wapo.view.R.color.headline_text_color))

        playerStatusText.text = itemView.resources.getString(com.wapo.view.R.string.audio_carousel_item_listen)
        playerDurationText.visibility = View.VISIBLE
        playerStatusProgressBar.visibility = View.GONE
    }

    override fun playState() {
        val audioBtn = itemView.findViewById<ConstraintLayout>(R.id.audio_button)
        audioBtn.background = ContextCompat.getDrawable(itemView.context, com.wapo.view.R.drawable.audio_pill_play)
        playerStatusIcon.setImageResource(com.wapo.view.R.drawable.pause_icon_btn)
        playerStatusIcon.visibility = View.VISIBLE
        playerDurationText.setTextColor(ContextCompat.getColor(itemView.context, com.wapo.flagship.features.audio.R.color.pill_duration_text))
        playerStatusText.text =
            itemView.resources.getString(com.wapo.view.R.string.audio_carousel_item_now_playing)
        playerStatusProgressBar.visibility = View.GONE
    }

    override fun loadingState() {
        playerStatusProgressBar.visibility = View.VISIBLE
        playerStatusIcon.visibility = View.INVISIBLE
    }

    fun onNowPlayingAudioItem(item: CarouselAudioViewItem, nowPlayingAudioItem: NowPlayingAudioItem?) {
        if (nowPlayingAudioItem == null) {
            readyState()
            return
        }

        if ((nowPlayingAudioItem.audioMediaConfig?.getPlayerType() != PlayerType.PODCAST && nowPlayingAudioItem.audioMediaConfig?.contentUrl != item.contentUrl)
            || (nowPlayingAudioItem.audioMediaConfig?.getPlayerType() == PlayerType.PODCAST && nowPlayingAudioItem.audioMediaConfig?.streamUrl?.getUrlWithoutParameters() != item.mediaUrl?.getUrlWithoutParameters())
        ) {
            readyState()
            return
        }

        when (nowPlayingAudioItem.audioPlaybackState) {
            is AudioPlaybackState.Playing -> playState()
            AudioPlaybackState.Buffering,
            AudioPlaybackState.JSONSourceInitializing,
            AudioPlaybackState.JSONSourceInitialized -> loadingState()
            else -> readyState()
        }
    }
}
