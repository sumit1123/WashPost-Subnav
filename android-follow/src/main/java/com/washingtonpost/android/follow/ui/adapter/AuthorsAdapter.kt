package com.washingtonpost.android.follow.ui.adapter

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.helper.FollowProvider
import com.washingtonpost.android.follow.ui.viewholder.AuthorViewHolder
import com.washingtonpost.android.follow.ui.viewholder.EmptyAuthorViewHolder
import kotlin.math.abs

class AuthorsAdapter(private val followProvider: FollowProvider) : PagedListAdapter<AuthorEntity, AuthorViewHolder>(DIFF_CALLBACK) {
    lateinit var selectionTracker: SelectionTracker<AuthorEntity>
    lateinit var selectionListener: AuthorSelectionListener
    var selectedItem: AuthorEntity? = null
        private set

    private var emptyRows = 0

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthorViewHolder {
        return when (viewType) {
            R.layout.author_item -> AuthorViewHolder.create(parent, followProvider)
            R.layout.author_empty_item -> EmptyAuthorViewHolder.create(parent, followProvider)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: AuthorViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.author_item -> {
                val item = getItem(position)
                updateSelection(holder, item)
                holder.bind(item, position)
            }
            R.layout.author_empty_item -> holder.bind(null, position)
        }
    }

    private fun updateSelection(holder: AuthorViewHolder, item: AuthorEntity?) {
        val isSelected = selectionTracker.isSelected(item)
        holder.isSelected = isSelected
        if (item == selectedItem && isSelected) {
            return
        }
        if (item != null && item == selectedItem && !isSelected && selectionTracker.selection.isEmpty) {
            holder.itemView.post {
                selectionTracker.select(item)
            }
            return
        }
        if (item != null && isSelected) {
            selectedItem = item
            selectionListener.onAuthorSelected(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        currentList?.let {
            if (position >= 0 && position < it.size) {
                if (getItem(position) != null) {
                    return R.layout.author_item
                }
            }
        }
        return R.layout.author_empty_item
    }

    fun getAuthor(position: Int): AuthorEntity? {
        currentList?.let {
            if (position >= 0 && position < it.size) {
                return getItem(position)
            }
        }
        return null
    }

    fun getPosition(author: AuthorEntity): Int {
        return currentList?.indexOf(author) ?: RecyclerView.NO_POSITION
    }

    override fun getItemId(position: Int): Long {
        currentList?.let {
            if (position >= 0 && position < it.size) {
                getItem(position)?.authorId?.hashCode()?.toLong()?.let { authorHash ->
                    return authorHash
                }
            }
        }
        return -1
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + emptyRows
    }

    fun setEmptyRows() {
        val rowsToAdd = if (super.getItemCount() < MIN_ITEMS && super.getItemCount() != 0)
            abs(super.getItemCount() - MIN_ITEMS) else 0
        while (emptyRows < rowsToAdd) {
            notifyItemInserted(super.getItemCount())
            emptyRows += 1
        }
        while (emptyRows > rowsToAdd) {
            notifyItemRemoved(super.getItemCount())
            emptyRows -= 1
        }
    }

    interface AuthorSelectionListener {
        fun onAuthorSelected(authorEntity: AuthorEntity)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AuthorEntity>() {
            override fun areItemsTheSame(oldItem: AuthorEntity, newItem: AuthorEntity): Boolean {
                return oldItem.authorId == newItem.authorId
            }

            override fun areContentsTheSame(oldItem: AuthorEntity, newItem: AuthorEntity): Boolean {
                return oldItem == newItem
            }
        }
        private const val MIN_ITEMS = 2
    }
}