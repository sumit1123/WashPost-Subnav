/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselAudioViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselBrightViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselEndCardViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselExternalViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselImmersionViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselSevenLiveViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselRecipeViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselVideoViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.EllipsisActionItem
import com.washingtonpost.android.recirculation.carousel.models.EmptyCardItem
import com.washingtonpost.android.recirculation.carousel.models.MyPostCarouselViewItem
import com.washingtonpost.android.recirculation.carousel.viewholders.ArticleStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.AudioStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.BrightStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.CarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.EmptyCardViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.ExternalStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.ImmersionEndCardStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.SevenLiveItemCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.ImmersionStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.MyPostStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.RecipeStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.viewholders.VideoStyleCarouselViewHolder
import com.washingtonpost.android.recirculation.carousel.views.CarouselRecyclerView
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.recirculation.databinding.EmptyItemBinding


const val VIEW_TYPE_STORY_CARD = 0
const val VIEW_TYPE_BRIGHT_CARD = 1
const val VIEW_TYPE_MY_POST_CARD = 2
const val VIEW_TYPE_VIDEO_CARD = 3
const val VIEW_TYPE_AUDIO_CARD = 4
const val VIEW_TYPE_IMMERSION_CARD = 5
const val VIEW_TYPE_END_CARD = 6
const val VIEW_TYPE_EMPTY_CARD = 7
const val VIEW_TYPE_RECIPE_CARD = 8
const val VIEW_TYPE_EXTERNAL_CARD = 9
const val VIEW_TYPE_SEVEN_LIVE_CARD = 10

class CarouselRecyclerViewAdapter(
    private val carouselItemWidth: Int,
    private val carouselItemHeight: Int,
    private val requestsHelper: CarouselProvider?,
    private val carouseClickedListener: OnCarouselClickedListener?,
    private val consumeTouchEventRule: CarouselView.CarouselConsumeTouchEventRule?,
    private val recyclerView: CarouselRecyclerView,
    private val shouldDisplayDateTime: Boolean,
    private val onAuthorClick: ((String) -> Unit)?,
    private val onSaveClick: ((String) -> Unit)?,
    private val onOptionsClick: ((String) -> Unit)?,
    private val onEllipsisClick: ((EllipsisActionItem) -> Unit)?,
    private var onBookmarkClick: ((EllipsisActionItem, ImageView, isStatusChecked: Boolean) -> Unit)? = null,
    private val onAudioIconClicked: ((MyPostCarouselViewItem) -> Unit)? = null,
    private val isNewsprint: Boolean = false,
    private val setResizeCarouselView: () -> Unit,
    private val resizeCarouselView: () -> Unit,
    private var getNowPlayingAudioItem: (() -> NowPlayingAudioItem?)?
) : RecyclerView.Adapter<CarouselViewHolder>() {
    private var items: List<CarouselViewItem> = listOf()
    private var mDownX = 0f
    private var mDownY = 0f
    private var mIsSwiping = false
    var onBindListener: ((Int) -> Unit)? = null
    var isAllExceptBlank = true

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): CarouselViewHolder {
        return when (viewType) {
            VIEW_TYPE_BRIGHT_CARD ->
                BrightStyleCarouselViewHolder(
                    layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_bright_item, viewGroup, false),
                    requestsHelper,
                    carouseClickedListener,
                    carouselItemWidth,
                    isAllExceptBlank,
                    setResizeCarouselView,
                    resizeCarouselView
                )
            VIEW_TYPE_MY_POST_CARD ->
                MyPostStyleCarouselViewHolder(
                    layoutInflaterFrom(viewGroup.context).inflate(R.layout.mypost_carousel_item, viewGroup, false),
                    carouselItemWidth,
                    shouldDisplayDateTime,
                    carouseClickedListener,
                    onAuthorClick,
                    onSaveClick,
                    onOptionsClick,
                    onAudioIconClicked
                )
            VIEW_TYPE_VIDEO_CARD ->
                VideoStyleCarouselViewHolder(
                    layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_video_item, viewGroup, false),
                    requestsHelper,
                    carouseClickedListener,
                    carouselItemWidth,
                    carouselItemHeight,
                    setResizeCarouselView,
                    resizeCarouselView
                )
            VIEW_TYPE_AUDIO_CARD ->
                AudioStyleCarouselViewHolder(
                    layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_audio_item, viewGroup, false),
                    requestsHelper,
                    carouseClickedListener,
                    carouselItemWidth,
                    carouselItemHeight,
                    onEllipsisClick,
                    items.map { it.contentUrl },
                    setResizeCarouselView,
                    resizeCarouselView,
                    getNowPlayingAudioItem
                )
            VIEW_TYPE_IMMERSION_CARD ->
                ImmersionStyleCarouselViewHolder(
                    layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_immersion_item, viewGroup, false),
                    requestsHelper,
                    carouseClickedListener,
                    carouselItemWidth,
                    isNewsprint,
                    setResizeCarouselView,
                    resizeCarouselView,
                )
            VIEW_TYPE_RECIPE_CARD ->
            {
                RecipeStyleCarouselViewHolder(
                    layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_recipe_item, viewGroup, false),
                    requestsHelper,
                    carouseClickedListener,
                    carouselItemWidth,
                    onBookmarkClick,
                    setResizeCarouselView,
                    resizeCarouselView
                )
            }
            VIEW_TYPE_END_CARD ->
                ImmersionEndCardStyleCarouselViewHolder(
                    layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_end_card, viewGroup, false),
                    requestsHelper,
                    carouseClickedListener
                )
            VIEW_TYPE_EXTERNAL_CARD -> ExternalStyleCarouselViewHolder(
                layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_external_item, viewGroup, false),
                requestsHelper,
                carouseClickedListener,
                carouselItemWidth,
                isNewsprint,
                setResizeCarouselView,
                resizeCarouselView,
            )
            VIEW_TYPE_SEVEN_LIVE_CARD -> SevenLiveItemCarouselViewHolder(
                layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_seven_live_item, viewGroup, false),
                requestsHelper,
                carouseClickedListener,
                carouselItemWidth,
                isNewsprint,
                setResizeCarouselView,
                resizeCarouselView,
            )
            VIEW_TYPE_EMPTY_CARD -> {
                val binding = EmptyItemBinding.inflate(
                    layoutInflaterFrom(viewGroup.context),
                    viewGroup,
                    false
                )
                EmptyCardViewHolder(binding)
            }
            else ->
                ArticleStyleCarouselViewHolder(
                    layoutInflaterFrom(viewGroup.context).inflate(R.layout.carousel_item, viewGroup, false),
                    requestsHelper,
                    carouseClickedListener,
                    setResizeCarouselView,
                    resizeCarouselView,
                    carouselItemWidth
                )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CarouselBrightViewItem -> VIEW_TYPE_BRIGHT_CARD
            is MyPostCarouselViewItem -> VIEW_TYPE_MY_POST_CARD
            is CarouselVideoViewItem -> VIEW_TYPE_VIDEO_CARD
            is CarouselAudioViewItem -> VIEW_TYPE_AUDIO_CARD
            is CarouselImmersionViewItem -> VIEW_TYPE_IMMERSION_CARD
            is CarouselRecipeViewItem -> VIEW_TYPE_RECIPE_CARD
            is CarouselEndCardViewItem -> VIEW_TYPE_END_CARD
            is CarouselSevenLiveViewItem -> VIEW_TYPE_SEVEN_LIVE_CARD
            is CarouselExternalViewItem -> VIEW_TYPE_EXTERNAL_CARD
            is EmptyCardItem -> VIEW_TYPE_EMPTY_CARD
            is CarouselViewItem -> VIEW_TYPE_STORY_CARD
            else -> error("Wrong item type ${items[position]}")
        }
    }

    private fun layoutInflaterFrom(context: Context?): LayoutInflater {
        return LayoutInflater.from(context)
    }

    override fun onBindViewHolder(carouselViewHolder: CarouselViewHolder, position: Int) {
        val item = items[position]
        carouselViewHolder.bind(item)
        onBindListener?.invoke(position)
        recyclerView.consumeTouchEventRule = consumeTouchEventRule
    }

    override fun onViewAttachedToWindow(holder: CarouselViewHolder) {
        super.onViewAttachedToWindow(holder)

        onNowPlayingAudioItem(getNowPlayingAudioItem?.invoke())
    }

    override fun onViewDetachedFromWindow(holder: CarouselViewHolder) {
        super.onViewDetachedFromWindow(holder)

        onNowPlayingAudioItem(getNowPlayingAudioItem?.invoke())
    }

    override fun onViewRecycled(holder: CarouselViewHolder) {
        super.onViewRecycled(holder)
        holder.onViewRecycled()
    }

    override fun getItemCount(): Int = items.size

    fun setItems(items: List<CarouselViewItem>) {
        this.items = items
    }

    fun setExcerptValue(isAllExceptBlank : Boolean)
    {
        this.isAllExceptBlank = isAllExceptBlank
    }

    fun onNowPlayingAudioItem(nowPlayingAudioItem: NowPlayingAudioItem?) {
        for (i in 0..itemCount - 1) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
            if (viewHolder is AudioStyleCarouselViewHolder) {
                (items.getOrNull(i) as? CarouselAudioViewItem)?.let { item ->
                    viewHolder.onNowPlayingAudioItem(item, nowPlayingAudioItem)
                }
            }
        }
    }

    /**
     * Interface methods to interact with the ViewHolder class from the application view classes.
     * Video related carousel view holders can implement these methods and then view classes can call these
     * interface methods based on the state of the video player.
     */
    interface MediaViewHolder {
        fun getVideoContainer(): ViewGroup?
        fun release()
        fun prepare()
        fun focus(onComplete:()->Unit = {})
    }

    interface AudioViewHolder {
        fun readyState()
        fun playState()
        fun loadingState()
    }
}