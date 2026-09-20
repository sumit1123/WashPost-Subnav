package com.wapo.flagship.features.articles2.viewholders

import android.content.Context
import android.content.res.TypedArray
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.washingtonpost.android.articles.R

/**
 * Style Helper for Context Box items
 */
class ContextBoxStyleHelper(
    context: Context,
) {
    val contextBoxTextStyle: Int
    val contextBoxHeaderStyle: Int

    init {

        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )

        contextBoxTextStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_context_box_text_style,
                R.style.ContextBoxText,
            )

        contextBoxHeaderStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_context_box_header_style,
                R.style.ContextBoxHeader,
            )

        typedArray.recycle()
    }
}
