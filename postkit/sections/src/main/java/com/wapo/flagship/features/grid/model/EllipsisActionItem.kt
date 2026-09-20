package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.playlist.Playlist

data class EllipsisActionItem(
    val menuType: EllipsisMenu,
    val url: String = "",
    val imageUrl: String = "",
    val byline: String = "",
    val headline: String = "",
    val articleList: List<String>? = null,
    val pageName: String = "",
    val articleLinkIsWebType: Boolean = false,
    val isAudioArticle : Boolean = true,
    val playlist: Playlist? = null,
    val audioMediaConfig: AudioMediaConfig? = null,
    val arcId: String = "",
    val contentType: String = ""
)