package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import com.washingtonpost.android.articles.R

object PullQuoteStyleHelper {
    private var pullQuoteStyle: Int = -1
    private var pullQuoteCaptionStyle = -1

    fun getPullQuoteStyle(context: Context): Int {
        if (pullQuoteStyle != -1) {
            return pullQuoteStyle
        }

        val typedArray = getTypedArray(context)
        pullQuoteStyle =
            typedArray
                .getResourceId(
                    R.styleable.ArticleItems_article_pull_quote_style,
                    R.style.ArticleText_PullQuote,
                ).also { typedArray.recycle() }

        return pullQuoteStyle
    }

    fun getPullQuoteCaptionStyle(context: Context): Int {
        if (pullQuoteCaptionStyle != -1) {
            return pullQuoteCaptionStyle
        }

        val typedArray = getTypedArray(context)
        pullQuoteCaptionStyle =
            typedArray
                .getResourceId(
                    R.styleable.ArticleItems_article_pull_quote_caption_style,
                    R.style.ArticleText_PullQuote_Caption,
                ).also { typedArray.recycle() }

        return pullQuoteCaptionStyle
    }

    private fun getTypedArray(context: Context): TypedArray =
        context.theme.obtainStyledAttributes(
            StylesHelper.getArticleItemStyle(context),
            R.styleable.ArticleItems,
        )
}
