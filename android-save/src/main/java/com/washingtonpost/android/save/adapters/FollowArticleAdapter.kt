package com.washingtonpost.android.save.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.isRecentMinutes
import com.washingtonpost.android.save.viewholders.FollowArticleViewHolder
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.MyPostArticleItem
import com.washingtonpost.android.save.models.PreviewItem.SectionPreviewItem
import com.wapo.view.RippleHelper
import com.washingtonpost.android.follow.helper.parseDisplayDate
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.TIME_STAMP_RECENCY_THRESHOLD

class FollowArticleAdapter(
    var sectionPreviewItem: SectionPreviewItem,
    val onArticleItemClick: (ArticleActionItem) -> Unit,
    val onOptionsClick: (ArticleActionItem) -> Unit
) : RecyclerView.Adapter<FollowArticleViewHolder>() {

    private val items = mutableListOf<MyPostArticleItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowArticleViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(com.washingtonpost.android.follow.R.layout.author_article_item, parent, false)
            .run {
                RippleHelper.addRippleEffectToView(this)
                FollowArticleViewHolder(this)
            }
    }

    override fun onBindViewHolder(holder: FollowArticleViewHolder, position: Int) {
        val article = items[position]
        holder.headlineView.text = article.headline
        val publishedTime = parseDisplayDate(article.displayDate)
        val time = if (publishedTime != null && isRecentMinutes(publishedTime, TIME_STAMP_RECENCY_THRESHOLD)) {
            DateUtils.getRelativeTimeSpanString(
                publishedTime,
                System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
            )
        } else {
            ""
        }
        holder.timeView.text = time
        holder.itemView.setOnClickListener {
            val myPostArticleItem = items[holder.adapterPosition]
            onArticleItemClick(ArticleActionItem(sectionPreviewItem.myPostSection, myPostArticleItem.contentUrl))
        }
        holder.menu.setOnClickListener {
            val myPostArticleItem = items[holder.adapterPosition]
            onOptionsClick(ArticleActionItem(sectionPreviewItem.myPostSection, myPostArticleItem.contentUrl))
        }
    }

    fun setItems(items: List<MyPostArticleItem>?) {
        this.items.clear()
        if (items != null) this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
