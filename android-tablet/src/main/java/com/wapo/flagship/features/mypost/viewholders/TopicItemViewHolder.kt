// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.mypost.viewholders

import android.graphics.drawable.Drawable
import com.wapo.android.commons.util.Logger
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.wapo.flagship.features.mypost.models.TopicFollowButtonActionItem
import com.wapo.flagship.features.mypost.models.TopicItemActionItem
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.models.MyPostTopicItem

class TopicItemViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {
    fun bind(
        item: MyPostTopicItem,
        onItemClick: (TopicItemActionItem) -> Unit,
        onFollowButtonClick: (TopicFollowButtonActionItem) -> Unit,
    ) {
        val imageView = itemView.findViewById<ImageView>(R.id.icon)
        Glide
            .with(imageView)
            .load(item.iconUrl)
            .listener(
                object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        imageView.visibility = View.GONE
                        Logger.e(TAG, e.toString())
                        e?.logRootCauses(TAG)
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        imageView.visibility = View.VISIBLE
                        return false
                    }
                },
            ).into(imageView)

        val textView = itemView.findViewById<TextView>(R.id.text)
        textView.text = item.text

        val followButton = itemView.findViewById<ImageButton>(R.id.button)
        if (item.isFollowing) {
            followButton.setImageDrawable(
                AppCompatResources.getDrawable(itemView.context, com.wapo.flagship.features.aixp.R.drawable.check),
            )
            followButton.background =
                AppCompatResources.getDrawable(
                    itemView.context,
                    R.drawable.topic_follow_circle_button_following,
                )
        } else {
            followButton.setImageDrawable(
                AppCompatResources.getDrawable(itemView.context, com.wpds.wpds.R.drawable.add),
            )
            followButton.background =
                AppCompatResources.getDrawable(
                    itemView.context,
                    R.drawable.topic_follow_circle_button_not_following,
                )
        }

        itemView.setOnClickListener {
            item.destinationUrl?.let {
                onItemClick(TopicItemActionItem(it))
            }
        }

        followButton.setOnClickListener {
            onFollowButtonClick(TopicFollowButtonActionItem(item.topicId))
        }
    }

    companion object {
        private val TAG = TopicItemViewHolder::class.java.simpleName
    }
}
