package com.washingtonpost.android.follow.ui.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.wapo.view.RippleHelper
import com.washingtonpost.android.follow.R
import com.washingtonpost.android.follow.model.ArticleItem

open class AbstractArticleItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var url: String? = null

    @CallSuper
    open fun bind(item: ArticleItem?, isNightModeOn: Boolean) {
        url = item?.url
        RippleHelper.addRippleEffectToView(itemView)
    }

    companion object {
        fun create(parent: ViewGroup): AbstractArticleItemHolder {
            val view = LayoutInflater.from(parent.context).inflate(com.wapo.view.R.layout.footer_item, parent, false)
            return AbstractArticleItemHolder(view)
        }
    }
}