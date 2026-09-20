package com.wapo.flagship.features.articles2.viewholders

import android.content.Context
import android.content.res.TypedArray
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.washingtonpost.android.articles.R

class ElementGroupStyleHelper(
    context: Context,
) {
    val textLinkBoxHeadlineStyle: Int
    val textLinkBoxKickerStyle: Int
    val textLinkBoxSubHeadlineStyle: Int
    val textLinkBoxDatelineStyle: Int
    val textLinkBoxItemStyle: Int
    val textLinkBoxItemSubheadStyle: Int

    init {

        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                StylesHelper.getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )

        textLinkBoxKickerStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_link_box_kicker_style,
                R.style.ArticleLinkBoxText_Kicker,
            )

        textLinkBoxHeadlineStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_link_box_headline_style,
                R.style.ArticleText_Headline_LinkBox,
            )

        textLinkBoxSubHeadlineStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_link_box_subheadline_style,
                R.style.ArticleLinkBoxText_SubHeadline,
            )

        textLinkBoxDatelineStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_link_box_dateline_style,
                R.style.ArticleLinkBoxText_Dateline,
            )

        textLinkBoxItemStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_link_box_text_style,
                R.style.ArticleLinkBoxText,
            )

        textLinkBoxItemSubheadStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_link_box_text_subhead_style,
                R.style.ArticleLinkBoxText_Subhead,
            )

        typedArray.recycle()
    }
}
