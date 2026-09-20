package com.washingtonpost.android.follow.ui

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.ui.adapter.AuthorsAdapter
import com.washingtonpost.android.follow.ui.viewholder.AuthorViewHolder

class AuthorDetails(private val adapterPosition: Int, val author: AuthorEntity?) :
        ItemDetailsLookup.ItemDetails<AuthorEntity>() {
    override fun getSelectionKey(): AuthorEntity? {
        return author
    }

    override fun getPosition(): Int {
        return adapterPosition
    }
}

class AuthorDetailsLookup(private val recyclerView: RecyclerView) :
        ItemDetailsLookup<AuthorEntity>() {
    override fun getItemDetails(e: MotionEvent): ItemDetails<AuthorEntity>? {
        return recyclerView.findChildViewUnder(e.x, e.y)?.let {
            (recyclerView.getChildViewHolder(it) as? AuthorViewHolder)?.getItemDetails()
        }
    }
}

class AuthorKeyProvider(var adapter: AuthorsAdapter) :
        ItemKeyProvider<AuthorEntity>(SCOPE_CACHED) {
    override fun getKey(position: Int): AuthorEntity? {
        return adapter.getAuthor(position)
    }

    override fun getPosition(key: AuthorEntity): Int {
        return adapter.getPosition(key)
    }
}