package com.wapo.flagship.features.grid.extensions

import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.EllipsisMenu

fun com.washingtonpost.android.recirculation.carousel.models.EllipsisActionItem.toEllipsisActionItem(
    menuType: EllipsisMenu
): EllipsisActionItem {
    return EllipsisActionItem(
        menuType = menuType,
        url = url,
        imageUrl = imageUrl,
        byline = byline,
        headline = headline,
        articleList = articleList,
        pageName = pageName,
        articleLinkIsWebType
    )
}