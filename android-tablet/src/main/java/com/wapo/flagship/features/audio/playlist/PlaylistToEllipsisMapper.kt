package com.wapo.flagship.features.audio.playlist

import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.EllipsisMenu

internal fun Playlist.toEllipsisActionItem(menuType: EllipsisMenu): EllipsisActionItem =
    EllipsisActionItem(
        menuType = menuType,
        url = this.contentUrl ?: this.id,
        playlist = this,
    )
