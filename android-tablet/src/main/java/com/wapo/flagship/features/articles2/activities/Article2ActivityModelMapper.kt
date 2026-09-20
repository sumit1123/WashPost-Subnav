package com.wapo.flagship.features.articles2.activities

import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.EllipsisMenu

/**
 * When Ellipsis is clicked from the Sections, Articles2 and Playlist Adapter. Mapping to Ellipsis action
 */

internal fun Article2.toEllipsisActionItem(
    url: String,
    arcId: String,
    contentType: String,
    imageUrl: String,
    headline: String
): EllipsisActionItem =
    EllipsisActionItem(
        menuType = EllipsisMenu.EllipsisButton,
        url = url,
        arcId = arcId,
        contentType = contentType,
        imageUrl = imageUrl,
        headline = headline
    )
