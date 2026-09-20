package com.wapo.flagship.features.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem
import com.washingtonpost.android.databinding.ContentPackBinding
import com.washingtonpost.android.databinding.ContentPackLargeBinding

class ContentPacksRecyclerViewAdapter(
    private val selectClicked: (ContentPackUiItem) -> Unit,
    private val done: () -> Unit,
    private val contentPacksViewModel: ContentPacksViewModel,
) : ListAdapter<ContentPackUiItem, RecyclerView.ViewHolder>(DiffUtils()) {
    /**
     * If viewType is Large, create [ContentPackItemLargeViewHolder].
     * Else if viewType is Normal, create [ContentPackItemViewHolder].
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == CONTENT_PACK_STYLE_LARGE) {
            val viewBinding = ContentPackLargeBinding.inflate(inflater, parent, false)
            ContentPackItemLargeViewHolder(
                viewBinding,
                selectClicked,
                done,
                contentPacksViewModel,
            )
        } else {
            val viewBinding = ContentPackBinding.inflate(inflater, parent, false)
            ContentPackItemViewHolder(
                viewBinding,
                selectClicked,
                done,
                contentPacksViewModel,
            )
        }
    }

    /**
     * Only the first content pack uses the Large, "double-wide" view; the rest are Normal.
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (position == 0) {
            val viewHolder: ContentPackItemLargeViewHolder = holder as ContentPackItemLargeViewHolder
            viewHolder.bind(getItem(position))
        } else {
            val viewHolder: ContentPackItemViewHolder = holder as ContentPackItemViewHolder
            viewHolder.bind(getItem(position))
        }
    }

    /**
     * Only the first content pack uses the Large, "double-wide" view; the rest are Normal.
     */
    override fun getItemViewType(position: Int): Int =
        if (position == 0) {
            CONTENT_PACK_STYLE_LARGE
        } else {
            CONTENT_PACK_STYLE_NORMAL
        }

    class DiffUtils : DiffUtil.ItemCallback<ContentPackUiItem>() {
        override fun areItemsTheSame(
            oldItem: ContentPackUiItem,
            newItem: ContentPackUiItem,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ContentPackUiItem,
            newItem: ContentPackUiItem,
        ): Boolean = oldItem == newItem
    }

    companion object {
        private const val CONTENT_PACK_STYLE_LARGE = 0
        private const val CONTENT_PACK_STYLE_NORMAL = 1
    }
}
