package com.washingtonpost.android.follow.ui.viewholder

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Glide
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.helper.FollowProvider
import com.washingtonpost.android.follow.ui.AuthorDetails
import kotlin.properties.Delegates

open class AuthorViewHolder(itemView: View, private val followProvider: FollowProvider) : RecyclerView.ViewHolder(itemView) {
    private var item: AuthorEntity? = null
    private val imageView: ImageView? = itemView.findViewById(R.id.image)
    private val nameView: AppCompatTextView? = itemView.findViewById(R.id.name)

    var isSelected: Boolean by Delegates.observable(false) { _, _, isActivated ->
        itemView.isActivated = isActivated
    }

    open fun bind(item: AuthorEntity?, position: Int) {
        val imageView = this.imageView ?: return
        val nameView = this.nameView ?: return
        this.item = item
        if (item == null) {
            itemView.setOnClickListener(null)
            itemView.visibility = View.GONE
        } else {
            if (imageView.tag != item.image) {
                followProvider.getAuthorImageRequestUrl(item.image)?.apply {
                    Glide.with(imageView)
                        .load(this)
                        .circleCrop()
                        .transition(GenericTransitionOptions.withNoTransition())
                        .error(R.drawable.author_placeholder)
                        .into(imageView)
                }
                imageView.tag = item.image
            }
            nameView.text = item.name
            itemView.visibility = View.VISIBLE
            val typeFace = if (isSelected) Typeface.BOLD else Typeface.NORMAL
            nameView.setTypeface(null, typeFace)
        }
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<AuthorEntity> = AuthorDetails(adapterPosition, item)

    companion object {
        fun create(parent: ViewGroup, followProvider: FollowProvider): AuthorViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.author_item, parent, false)
            return AuthorViewHolder(view, followProvider)
        }
    }
}