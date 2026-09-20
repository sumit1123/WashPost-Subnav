package com.washingtonpost.android.follow.ui.viewholder

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.helper.parseDisplayDate
import com.washingtonpost.android.follow.model.ArticleItem
import com.washingtonpost.android.follow.ui.adapter.ArticlesAdapter

open class ArticleItemHolder(
    itemView: View,
    private val articleClickListener: ArticlesAdapter.ArticleClickListener,
    private val utilityMenuCallback: (ArticleItem?) -> Unit
) : AbstractArticleItemHolder(itemView) {

    private val headline = itemView.findViewById<TextView>(R.id.headline)
    private val time = itemView.findViewById<TextView>(R.id.time)
    private val utilityMenu = itemView.findViewById<ImageButton>(R.id.ib_utility_menu)

    override fun bind(item: ArticleItem?, isNightModeOn: Boolean) {
        super.bind(item, isNightModeOn)
        setHeadline(item)
        setTime(item)
        itemView.setOnClickListener {
            item?.url?.let {
                articleClickListener.onArticleClicked(it)
            }
        }
        utilityMenu.setOnClickListener {
            utilityMenuCallback.invoke(item)
        }
    }

    private fun setHeadline(item: ArticleItem?) {
        headline.text = item?.headline
    }

    private fun setTime(item: ArticleItem?) {
        val publishedTime = parseDisplayDate(item?.displayDate)
        if (publishedTime != null) {
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                publishedTime,
                System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
            )
            time.text = relativeTime
        } else {
            time.text = null
        }
    }

    companion object {
        private val TAG = ArticleItemHolder::class.java.simpleName

        fun create(
            parent: ViewGroup,
            articleClickListener: ArticlesAdapter.ArticleClickListener,
            utilityMenuCallback: (ArticleItem?) -> Unit
        ): AbstractArticleItemHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.author_article_item, parent, false)
            return ArticleItemHolder(view, articleClickListener, utilityMenuCallback)
        }
    }
}