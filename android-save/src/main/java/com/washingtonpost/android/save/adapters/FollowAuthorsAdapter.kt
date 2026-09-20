package com.washingtonpost.android.save.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.save.viewholders.BaseFollowAuthorViewHolder
import com.washingtonpost.android.save.viewholders.FollowAuthorViewHolder
import com.washingtonpost.android.save.viewholders.FollowEmptyViewHolder
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.save.R
import kotlin.math.min

private const val VIEW_TYPE_EMPTY = 1

class FollowAuthorsAdapter : RecyclerView.Adapter<BaseFollowAuthorViewHolder>() {

    private val items = mutableListOf<AuthorEntity>()

    var selectedItem = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onItemSelected: ((Int) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseFollowAuthorViewHolder {
        return if (viewType == VIEW_TYPE_EMPTY) {
            LayoutInflater.from(parent.context)
                .inflate(com.washingtonpost.android.follow.R.layout.author_empty_item, parent, false)
                .run { FollowEmptyViewHolder(this) }
        } else {
            LayoutInflater.from(parent.context)
                .inflate(com.washingtonpost.android.follow.R.layout.author_item, parent, false)
                .run { FollowAuthorViewHolder(this) }
        }

    }

    override fun onBindViewHolder(holder: BaseFollowAuthorViewHolder, position: Int) {
        holder.bind(items, position, this)
    }

    override fun getItemCount(): Int {
        if (items.size == 1) {
            return 2
        }
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= items.size) {
            return VIEW_TYPE_EMPTY
        }
        return super.getItemViewType(position)
    }

    override fun getItemId(position: Int): Long {
        if (position >= items.size) {
            return RecyclerView.NO_ID
        }
        return items[position].authorId.hashCode().toLong()
    }

    fun setItems(items: List<AuthorEntity>?) {
        this.items.clear()
        if (items != null) {
            this.items.addAll(items)
        }
        selectedItem = min(selectedItem, this.items.size - 1)
        notifyDataSetChanged()
    }

}