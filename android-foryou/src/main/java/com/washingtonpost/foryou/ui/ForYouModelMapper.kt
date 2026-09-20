package com.washingtonpost.foryou.ui

import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.data.byline
import com.washingtonpost.foryou.data.getURL
import com.washingtonpost.foryou.data.getVideoShareUrl

internal fun RecommendationsItem.toEllipsisActionItem() : EllipsisActionItem {
    return EllipsisActionItem(
        menuType = EllipsisMenu.ForYouEllipsisButton,
        url = this.getURL(),
        imageUrl = this.imageUrl ?: "",
        byline = this.byline(),
        headline = this.headline ?: "",
        pageName = ForYouFragment.FOR_YOU_DISPLAY_NAME,
        isAudioArticle = this.additionalProperties?.audioArticle?.enabled == true
    )
}

internal fun RecommendationsItem.toEllipsisVideoActionItem() : EllipsisActionItem {
    return EllipsisActionItem(
        menuType = EllipsisMenu.VideoActionButton,
        url = this.getVideoShareUrl(),
        imageUrl = this.imageUrl ?: "",
        byline = this.byline(),
        headline = this.headlines?.basic ?: this.headline ?: this.video?.altText ?: "",
        pageName = ForYouFragment.FOR_YOU_DISPLAY_NAME,
        isAudioArticle = this.additionalProperties?.audioArticle?.enabled == true
    )
}