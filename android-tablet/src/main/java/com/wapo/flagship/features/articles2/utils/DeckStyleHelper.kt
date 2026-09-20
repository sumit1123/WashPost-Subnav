package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import com.washingtonpost.android.articles.R

object DeckStyleHelper {
    fun getDeckStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_deck_style,
                R.style.ArticleText_Deck,
            ).also { typedArray.recycle() }
    }
}
