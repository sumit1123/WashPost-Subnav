package com.wapo.flagship.features.articles2.itemdecorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.viewholders.*
import com.washingtonpost.android.articles.R
import com.wapo.flagship.features.articles.recirculation.Articles2RecirculationViewHolder

/**
 * This item decoration is used to add margins for the recycler view items for Article content.
 */
class MarginItemDecoration : RecyclerView.ItemDecoration() {
    /**
     * Please refer [RecyclerView.ItemDecoration.getItemOffsets] for more info on this one.
     */
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val resources = view.resources
        val holder = parent.findContainingViewHolder(view)
        // No decoration if view type is unknown. Article's view item types are starting from 1.
        // Also unknown items are moved in ArticleContentNativeViewHolder.
        if ((holder?.itemViewType ?: -1) < Articles2ItemsRecyclerViewAdapter.KICKER_TYPE) {
            return
        }
        var explicitlySetMargins = false
        with(outRect) {
            // Instead of adding the top margin to the kicker (which is the first element), we can make sure that any top
            // element in the list of items will have the article top margin.
            // E.g. in some opinion articles we do not have kicker and header becomes the first element.
            top =
                if (parent.getChildAdapterPosition(view) == 0) {
                    if (holder is SubNavViewHolder) {
                        resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                    } else {
                        resources.getDimensionPixelSize(R.dimen.articles_medium_margin)
                    }
                } else {
                    when (holder) {
                        is SanitizedHtmlViewHolder -> {
                            if (holder.isSingleNumber || holder.isBriefExclusiveLabel) {
                                resources.getDimensionPixelSize(R.dimen.articles_large_margin)
                            } else {
                                resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                            }
                        }

                        is ForYouRecirculationViewHolder ->
                            resources.getDimensionPixelSize(
                                R.dimen.articles_large_margin,
                            )
                        is DividerViewHolder ->
                            resources.getDimensionPixelSize(
                                R.dimen.articles_small_margin,
                            )
                        else -> resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                    }
                }

            val isTablet = AppContextUtils.isTablet()

            var finalLeft = 0
            var finalRight = 0
            var articleMargin = AppContextUtils.calculateArticleMargin()

            val sideSpace =
                when (holder) {
                    is AdViewHolder, is ForYouRecirculationViewHolder, is InStoryRecirculationViewHolder, is Articles2RecirculationViewHolder, is SubNavViewHolder -> {
                        if (isTablet) {
                            finalLeft = resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                            finalRight = resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                            explicitlySetMargins=true
                        }
                        resources.getDimensionPixelSize(R.dimen.articles_medium_margin)
                    }
                    else ->
                    resources.getDimensionPixelOffset(R.dimen.articles_medium_margin)
                }
            left = sideSpace
            right = sideSpace

            bottom =
                when (holder) {
                    is KickerViewHolder, is ByLineViewHolder, is DeckViewHolder, is TitleViewHolder, is ElevatedBylineViewHolder -> {
                        finalLeft = articleMargin
                        finalRight = articleMargin
                        resources.getDimensionPixelSize(
                            R.dimen.articles_small_margin,
                        )
                    }

                    is DateViewHolder ->
                        if (holder.isExpandedByline) {
                            resources.getDimensionPixelSize(R.dimen.articles_small_margin)
                        } else {
                            resources.getDimensionPixelSize(R.dimen.articles_large_margin)
                        }

                    is VideoViewHolder -> {
                        if (holder.isVerticalVideo) {
                            resources.getDimensionPixelSize(R.dimen.articles_large_margin).also {
                                finalLeft = it
                                finalRight = it
                            }
                        } else {
                            resources.getDimensionPixelSize(R.dimen.articles_no_margin).also {
                                finalLeft = it
                                finalRight = it
                            }
                        }
                        // This is returned. Above is just a statement.
                        resources.getDimensionPixelSize(R.dimen.articles_large_margin)
                    }

                    is ImageViewHolder, is FtsCarouselRecirculationViewHolder -> {
                        // As per the design docs we don't need any horizontal margin for these two type of items.
                        finalLeft = resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                        finalRight = resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                        explicitlySetMargins = true

                        // This is returned. Above is just a statement.
                        resources.getDimensionPixelSize(R.dimen.articles_large_margin)
                    }

                    /***
                     * As pet the UI Tweak requirements  for the number and subhead to be inline
                     * Setting the bottom margin & left alignment for single number {The Brief }
                     *  Making the margin negative to align with the number and subhead , Moving the subhead to right to fit the single number
                     */
                    is SanitizedHtmlViewHolder -> {
                        if (holder.isSingleNumber) {
                            finalLeft =
                                resources.getDimensionPixelOffset(
                                    R.dimen.articles_brief_number_left_margin,
                                )
                            resources.getDimensionPixelOffset(
                                R.dimen.articles_brief_number_bottom_margin,
                            )
                        } else if (holder.isBriefExclusiveLabel) {
                            finalLeft =
                                resources.getDimensionPixelOffset(
                                    R.dimen.articles_brief_number_left_margin,
                                )
                            resources.getDimensionPixelOffset(
                                R.dimen.articles_brief_exclusive_label_bottom_margin,
                            )
                        } else if (holder.isBrief) {
                            finalLeft =
                                resources.getDimensionPixelOffset(
                                    R.dimen.articles_brief_large_margin,
                                ) + articleMargin
                            finalRight = articleMargin
                            explicitlySetMargins = true
                            resources.getDimensionPixelOffset(R.dimen.articles_large_margin)
                        } else if (holder.isExpandedByline) {
                            resources.getDimensionPixelOffset(R.dimen.articles_medium_margin)
                        } else {
                            finalLeft =
                                resources.getDimensionPixelOffset(R.dimen.articles_medium_margin)
                            resources.getDimensionPixelOffset(R.dimen.articles_large_margin)
                        }
                    }

                    is ListViewHolder -> {
                        if (holder.isBrief) {
                            finalLeft =
                                resources.getDimensionPixelOffset(
                                    R.dimen.articles_brief_medium_margin,
                                ) + articleMargin
                            finalRight = articleMargin
                            explicitlySetMargins = true
                            resources.getDimensionPixelOffset(R.dimen.articles_large_margin)
                        } else {
                            resources.getDimensionPixelOffset(R.dimen.articles_medium_margin)
                        }
                    }

                    is AnchorViewHolder -> resources.getDimensionPixelSize(R.dimen.articles_no_margin)

                    is DividerViewHolder, is CommentsViewHolder ->
                        resources.getDimensionPixelSize(
                            R.dimen.articles_large_margin,
                        )

                    is StandaloneAudioViewHolder, is AudioViewHolder ->
                        resources.getDimensionPixelOffset(
                            R.dimen.articles_large_margin,
                        )

                    is AdViewHolder -> {
                        explicitlySetMargins = true
                        finalLeft = resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                        finalRight = resources.getDimensionPixelSize(R.dimen.articles_no_margin)
                        resources.getDimensionPixelOffset(R.dimen.articles_medium_margin)

                    }
                    else -> resources.getDimensionPixelOffset(R.dimen.articles_medium_margin)
                }

            if (!explicitlySetMargins) {
                finalLeft = articleMargin
                finalRight = articleMargin

            }
            left = finalLeft
            right = finalRight
        }
    }
}
