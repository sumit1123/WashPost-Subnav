/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import com.washingtonpost.android.articles.R

object StylesHelper {

    fun getArticleItemStyle(context: Context): Int {
        val typedArray: TypedArray = context.theme.obtainStyledAttributes(
            null,
            R.styleable.ArticlesRecyclerView,
            0, 0
        )
        return typedArray.getResourceId(
            R.styleable.ArticlesRecyclerView_article_items_style,
            R.style.ArticleItemsStyle
        ).also { typedArray.recycle() }
    }

    fun isAllCaps(resId: Int, context: Context): Boolean {
        var a: TypedArray? = null
        return try {
            a = context.obtainStyledAttributes(resId, intArrayOf(android.R.attr.textAllCaps))
            a.getBoolean(0, false)
        } finally {
            a?.recycle()
        }
    }

}