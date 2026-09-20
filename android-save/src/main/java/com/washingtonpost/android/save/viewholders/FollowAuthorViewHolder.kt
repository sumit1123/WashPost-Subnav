package com.washingtonpost.android.save.viewholders

import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.save.adapters.FollowAuthorsAdapter

open class BaseFollowAuthorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    open fun bind(
        items: MutableList<AuthorEntity>,
        position: Int,
        followAuthorsAdapter: FollowAuthorsAdapter
    ) {

    }
}

class FollowAuthorViewHolder(itemView: View) : BaseFollowAuthorViewHolder(itemView) {
    var isSelected = false
    val nameView = itemView.findViewById<TextView>(R.id.name)
    val imageView = itemView.findViewById<ImageView>(R.id.image)
    val cardView = itemView.findViewById<MaterialCardView>(R.id.card_view)

    override fun bind(
        items: MutableList<AuthorEntity>,
        position: Int,
        followAuthorsAdapter: FollowAuthorsAdapter
    ) {
        super.bind(items, position, followAuthorsAdapter)

        isSelected = position == followAuthorsAdapter.selectedItem
        cardView.isActivated = isSelected

        val author = items[position]

        nameView.text = author.name
        val typeFace = if (isSelected) Typeface.BOLD else Typeface.NORMAL
        nameView.setTypeface(null, typeFace)
        if (imageView.tag != author.image) {
            Glide.with(imageView)
                .load(author.image)
                .circleCrop()
                .transition(GenericTransitionOptions.withNoTransition())
                .error(R.drawable.author_placeholder)
                .into(imageView)
            imageView.tag = author.image
        }

        itemView.setOnClickListener {
            if (adapterPosition >= 0) {
                followAuthorsAdapter.onItemSelected?.invoke(adapterPosition)
                followAuthorsAdapter.selectedItem = adapterPosition
            }
        }
    }
}

class FollowEmptyViewHolder(itemView: View) : BaseFollowAuthorViewHolder(itemView)