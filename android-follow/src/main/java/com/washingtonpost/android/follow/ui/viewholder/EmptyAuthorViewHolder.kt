package com.washingtonpost.android.follow.ui.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.helper.FollowProvider

class EmptyAuthorViewHolder(itemView: View, followProvider: FollowProvider) : AuthorViewHolder(itemView, followProvider) {
    override fun bind(item: AuthorEntity?, position: Int) {
        // do nothing
    }

    companion object {
        fun create(parent: ViewGroup, followProvider: FollowProvider): EmptyAuthorViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.author_empty_item, parent, false)
            return EmptyAuthorViewHolder(view, followProvider)
        }
    }
}