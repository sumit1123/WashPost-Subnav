package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import com.wapo.view.habittiles.ArticleSource

data class Audio(
    val mediaId: String? = null,
    val streamUrl: String? = null,
    val streamUrlNoAds: String? = null,
    val duration: Long? = null,
    val slug: String? = null,
    val displayDate: String? = null,
    val displayLabel: String? = null,
    val displayTransparency: String? = null,
    val titlePrefix: String? = null,
    val title: String? = null,
    val coverImage: String? = null,
    val subscriptionLinks: SubscriptionLinks? = null,
    val tracking: AudioTracking? = null,
    val inlinePlayer: InlinePlayer? = null,
    val playerType: PlayerType,
    val playerMediaEntity: Media? = null,
    val transcriptUrl: String? = null,
    val sources: List<ArticleSource>? = null,
    val series: String? = null,
    val seriesName: String? = null,
    val language: String? = null,
    val adConfig: AudioMediaAdConfig? = null,
)