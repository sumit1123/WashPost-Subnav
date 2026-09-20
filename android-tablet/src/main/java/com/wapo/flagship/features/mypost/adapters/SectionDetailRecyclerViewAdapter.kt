package com.wapo.flagship.features.mypost.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.washingtonpost.android.save.databinding.MyPostBannerBinding
import com.washingtonpost.android.save.databinding.MyPostDetailHeaderBinding
import com.washingtonpost.android.save.databinding.MyPostDetailReadingListFooterBinding
import com.washingtonpost.android.save.databinding.MyPostEmptyStateBinding
import com.washingtonpost.android.save.databinding.MyPostSectionDetailBinding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.DetailItem
import com.washingtonpost.android.save.models.DetailItem.Article
import com.washingtonpost.android.save.models.DetailItem.Empty
import com.washingtonpost.android.save.models.DetailItem.FooterArchive
import com.washingtonpost.android.save.models.DetailItem.Header
import com.washingtonpost.android.save.viewholders.*
import java.lang.IllegalStateException

class SectionDetailRecyclerViewAdapter(
    private val onArticleItemClick: (ArticleActionItem) -> Unit,
    private val onSaveClick: (ArticleActionItem) -> Unit,
    private val onOptionsClick: (ArticleActionItem) -> Unit,
    private val onSignInClick: () -> Unit,
    private val onSettingsClick: () -> Unit,
    private val onViewArchiveClick: () -> Unit,
    private val onUpdateConsentSettingsClick: () -> Unit,
) : ListAdapter<DetailItem, DetailsItemViewHolder>(DiffUtils()) {
    enum class ItemType {
        ARTICLE,
        PODCAST,
        HEADER,
        FOOTER_ARCHIVE,
        EMPTY
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DetailsItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ItemType.ARTICLE.ordinal -> {
                val viewBinding = MyPostSectionDetailBinding.inflate(inflater, parent, false)
                SectionDetailArticleViewHolder(
                    viewBinding,
                    onArticleItemClick,
                    onSaveClick,
                    onOptionsClick,
                )
            }
            ItemType.HEADER.ordinal -> {
                val viewBinding =
                    MyPostDetailHeaderBinding.inflate(inflater, parent, false)
                SectionDetailHeaderViewHolder(viewBinding)
            }
            ItemType.FOOTER_ARCHIVE.ordinal -> {
                val viewBinding =
                    MyPostDetailReadingListFooterBinding.inflate(inflater, parent, false)
                SectionDetailReadingListFooterViewHolder(viewBinding, onViewArchiveClick)
            }
            ItemType.EMPTY.ordinal -> {
                val viewBinding =
                    MyPostEmptyStateBinding.inflate(inflater, parent, false)
                EmptyDetailItemViewHolder(
                    viewBinding,
                    onSignInClick,
                    onSettingsClick,
                    onUpdateConsentSettingsClick
                )
            }
            else -> {
                throw IllegalStateException("Unknown viewType $viewType")
            }
        }
    }

    override fun onBindViewHolder(
        holder: DetailsItemViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int = getItemViewType(getItem(position))

    class DiffUtils : DiffUtil.ItemCallback<DetailItem>() {
        override fun areItemsTheSame(
            oldItem: DetailItem,
            newItem: DetailItem,
        ): Boolean = getItemViewType(oldItem) == getItemViewType(newItem)

        override fun areContentsTheSame(
            oldItem: DetailItem,
            newItem: DetailItem,
        ): Boolean = oldItem == newItem
    }

    companion object {
        @JvmStatic
        private fun getItemViewType(detailItem: DetailItem): Int =
            when (detailItem) {
                is Article -> ItemType.ARTICLE.ordinal
                is Header -> ItemType.HEADER.ordinal
                is FooterArchive -> ItemType.FOOTER_ARCHIVE.ordinal
                is Empty -> ItemType.EMPTY.ordinal
            }
    }
}
