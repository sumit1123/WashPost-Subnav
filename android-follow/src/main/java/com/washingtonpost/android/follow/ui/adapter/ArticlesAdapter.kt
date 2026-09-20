package com.washingtonpost.android.follow.ui.adapter

import android.graphics.Color
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.wapo.flagship.features.nightmode.NightModeController
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.helper.FollowProvider
import com.washingtonpost.android.follow.model.ArticleItem
import com.washingtonpost.android.follow.repository.NetworkState
import com.washingtonpost.android.follow.ui.viewholder.AbstractArticleItemHolder
import com.washingtonpost.android.follow.ui.viewholder.ArticleItemHolder
import com.washingtonpost.android.follow.ui.viewholder.NetworkStateItemViewHolder

class ArticlesAdapter(
    private val followProvider: FollowProvider,
    private val articleClickListener: ArticleClickListener,
    private val retryCallback: () -> Unit,
    private val utilityMenuCallback: (ArticleItem?) -> Unit
)
    : PagedListAdapter<ArticleItem, AbstractArticleItemHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    private var networkState: NetworkState? = null
    override fun onBindViewHolder(holder: AbstractArticleItemHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.network_state_item -> (holder as NetworkStateItemViewHolder).bind(networkState)
            else -> {
                val isNightMode = (holder.itemView.context.applicationContext as? NightModeController)?.isNightModeEnabled() ?: false
                holder.bind(getItem(position), isNightMode)
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    override fun onBindViewHolder(
            holder: AbstractArticleItemHolder,
            position: Int,
            payloads: MutableList<Any>) {
        onBindViewHolder(holder, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractArticleItemHolder {
        return when (viewType) {
            com.wapo.view.R.layout.footer_item -> AbstractArticleItemHolder.create(parent)
            com.wapo.view.R.layout.sf_module -> ArticleItemHolder.create(parent, articleClickListener, utilityMenuCallback)
            R.layout.network_state_item -> NetworkStateItemViewHolder.create(parent, retryCallback)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.network_state_item
        } else {
            getItem(position) ?: return com.wapo.view.R.layout.footer_item
            return com.wapo.view.R.layout.sf_module
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    override fun getItemId(position: Int): Long {
        currentList?.let {
            if (position >= 0 && position < it.size) {
                getItem(position)?.id?.hashCode()?.toLong()?.let { articleHash ->
                    return articleHash
                }
            }
        }
        return -1
    }

    fun setNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newNetworkState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    interface ArticleClickListener {
        fun onArticleClicked(url: String)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ArticleItem>() {
            override fun areItemsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean =
                    oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean =
                    oldItem == newItem
        }
    }
}