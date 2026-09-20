package com.wapo.flagship.features.mypost.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.wapo.flagship.features.mypost.models.TopicFollowButtonActionItem
import com.wapo.flagship.features.mypost.models.TopicItemActionItem
import com.wapo.flagship.features.mypost.viewholders.BannerPreviewItemViewHolder
import com.wapo.flagship.features.mypost.viewholders.BirthdayFrontPagePreviewItemViewHolder
import com.wapo.flagship.features.mypost.viewholders.FooterPreviewItemViewHolder
import com.wapo.flagship.features.mypost.viewholders.TopicsPreviewViewHolder
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.save.databinding.MyPostBannerBinding
import com.washingtonpost.android.save.databinding.MyPostBirthdayFrontPageBinding
import com.washingtonpost.android.save.databinding.MyPostEmptyStateBinding
import com.washingtonpost.android.save.databinding.MyPostFollowPreviewBinding
import com.washingtonpost.android.save.databinding.MyPostPreviewFooterBinding
import com.washingtonpost.android.save.databinding.MyPostSectionPreviewBinding
import com.washingtonpost.android.save.databinding.MyPostTopicsPreviewBinding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.models.PreviewItem.BannerPreviewItem
import com.washingtonpost.android.save.models.PreviewItem.EmptyPreviewItem
import com.washingtonpost.android.save.models.PreviewItem.FooterPreviewItem
import com.washingtonpost.android.save.models.PreviewItem.SectionPreviewItem
import com.washingtonpost.android.save.types.MyPostSection
import com.washingtonpost.android.save.viewholders.EmptyPreviewItemViewHolder
import com.washingtonpost.android.save.viewholders.FollowingPreviewItemViewHolder
import com.washingtonpost.android.save.viewholders.ItemViewHolder
import com.washingtonpost.android.save.viewholders.PurchasedArticleViewHolder
import com.washingtonpost.android.save.viewholders.ReadingHistoryPreviewItemViewHolder
import com.washingtonpost.android.save.viewholders.ReadingListPreviewItemViewHolder

class SectionsPreviewRecyclerViewAdapter(
    private val onArticleItemClick: (ArticleActionItem) -> Unit,
    private val onSaveClick: (ArticleActionItem) -> Unit,
    private val onOptionsClick: (ArticleActionItem) -> Unit,
    private val onViewMoreClick: (MyPostSection) -> Unit,
    private val onMoreFromAuthorClick: (AuthorItem) -> Unit,
    private val onSignInClick: () -> Unit,
    private val onSettingsClick: () -> Unit,
    private val onOpenSectionClick: (String) -> Unit,
    private val onViewArchiveClick: () -> Unit,
    private val onUpdateConsentSettingsClick: () -> Unit,
    private val onInterestItemClick: (TopicItemActionItem) -> Unit,
    private val onFollowButtonClick: (TopicFollowButtonActionItem) -> Unit,
    private val onBirthdayFrontPageClick: () -> Unit,
    private val onBannerEvent: (BannerEvent) -> Unit
) : ListAdapter<PreviewItem, ItemViewHolder>(DiffUtils()) {
    enum class ItemType {
        SAVED_STORIES,
        TOPICS,
        FOLLOWING,
        READING_HISTORY,
        FOOTER,
        EMPTY,
        BANNER,
        BIRTHDAY_FRONT_PAGE,
        PURCHASED_ARTICLES
    }

    var onAuthorDataRequested: ((String) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ItemType.SAVED_STORIES.ordinal -> {
                val viewBinding =
                    MyPostSectionPreviewBinding.inflate(inflater, parent, false)
                ReadingListPreviewItemViewHolder(
                    viewBinding,
                    onArticleItemClick = onArticleItemClick,
                    onOptionsClick = onOptionsClick,
                    onViewMoreClick = onViewMoreClick,
                    onSaveClick = onSaveClick,
                )
            }

            ItemType.TOPICS.ordinal -> {
                val viewBinding =
                    MyPostTopicsPreviewBinding.inflate(inflater, parent, false)
                TopicsPreviewViewHolder(
                    viewBinding,
                    onInterestItemClick = onInterestItemClick,
                    onFollowButtonClick = onFollowButtonClick,
                    onViewMoreClick = onViewMoreClick,
                )
            }

            ItemType.FOLLOWING.ordinal -> {
                val viewBinding =
                    MyPostFollowPreviewBinding.inflate(inflater, parent, false)
                FollowingPreviewItemViewHolder(
                    viewBinding,
                    onArticleItemClick = onArticleItemClick,
                    onOptionsClick = onOptionsClick,
                    onViewMoreClick = onViewMoreClick,
                    onMoreFromAuthorClick = onMoreFromAuthorClick,
                    onAuthorDataRequested = onAuthorDataRequested,
                )
            }

            ItemType.READING_HISTORY.ordinal -> {
                val viewBinding =
                    MyPostSectionPreviewBinding.inflate(inflater, parent, false)
                ReadingHistoryPreviewItemViewHolder(
                    viewBinding,
                    onArticleItemClick = onArticleItemClick,
                    onOptionsClick = onOptionsClick,
                    onViewMoreClick = onViewMoreClick,
                )
            }

            ItemType.FOOTER.ordinal -> {
                val viewBinding = MyPostPreviewFooterBinding.inflate(inflater, parent, false)
                FooterPreviewItemViewHolder(viewBinding, onOpenSectionClick)
            }

            ItemType.EMPTY.ordinal -> {
                val viewBinding = MyPostEmptyStateBinding.inflate(inflater, parent, false)
                EmptyPreviewItemViewHolder(
                    viewBinding,
                    onSignInClick,
                    onSettingsClick,
                    onViewArchiveClick,
                    onUpdateConsentSettingsClick
                )
            }

            ItemType.BANNER.ordinal -> {
                val viewBinding = MyPostBannerBinding.inflate(inflater, parent, false)
                BannerPreviewItemViewHolder(
                    viewBinding,
                    onBannerEvent,
                )
            }

            ItemType.BIRTHDAY_FRONT_PAGE.ordinal -> {
                val viewBinding = MyPostBirthdayFrontPageBinding.inflate(inflater, parent, false)
                BirthdayFrontPagePreviewItemViewHolder(
                    viewBinding,
                    onBirthdayFrontPageClick,
                )
            }

            ItemType.PURCHASED_ARTICLES.ordinal -> {
                val viewBinding =
                    MyPostSectionPreviewBinding.inflate(inflater, parent, false)
                PurchasedArticleViewHolder(
                    viewBinding,
                    onArticleItemClick,
                    onViewMoreClick,
                    onOptionsClick
                )
            }

            else -> {
                throw IllegalStateException("Unknown viewType $viewType")
            }
        }
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    override fun onViewAttachedToWindow(holder: ItemViewHolder) {
        super.onViewAttachedToWindow(holder)

        if (holder is BannerPreviewItemViewHolder) {
            holder.attributionInfo?.let {
                onBannerEvent.invoke(
                    BannerEvent.ImpressionEvent(
                        BannerLifecycleEvent.StartImpression(
                            it
                        )
                    )
                )
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ItemViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.unbind()
    }

    override fun getItemViewType(position: Int): Int = getItemViewType(getItem(position))

    class DiffUtils : DiffUtil.ItemCallback<PreviewItem>() {
        override fun areItemsTheSame(
            oldItem: PreviewItem,
            newItem: PreviewItem,
        ): Boolean = getItemViewType(oldItem) == getItemViewType(newItem)

        override fun areContentsTheSame(
            oldItem: PreviewItem,
            newItem: PreviewItem,
        ): Boolean = oldItem == newItem
    }

    companion object {
        @JvmStatic
        private fun getItemViewType(previewItem: PreviewItem): Int =
            when (previewItem) {
                is SectionPreviewItem -> {
                    when (previewItem.myPostSection) {
                        MyPostSection.SAVED_STORIES -> ItemType.SAVED_STORIES.ordinal
                        MyPostSection.TOPICS -> ItemType.TOPICS.ordinal
                        MyPostSection.FOLLOWING -> ItemType.FOLLOWING.ordinal
                        MyPostSection.READING_HISTORY -> ItemType.READING_HISTORY.ordinal
                        MyPostSection.PURCHASE -> ItemType.PURCHASED_ARTICLES.ordinal
                        else -> -1
                    }
                }

                is FooterPreviewItem -> ItemType.FOOTER.ordinal
                is EmptyPreviewItem -> ItemType.EMPTY.ordinal
                is BannerPreviewItem -> ItemType.BANNER.ordinal
                is PreviewItem.BirthdayFrontPagePreviewItem -> ItemType.BIRTHDAY_FRONT_PAGE.ordinal
            }
    }
}
