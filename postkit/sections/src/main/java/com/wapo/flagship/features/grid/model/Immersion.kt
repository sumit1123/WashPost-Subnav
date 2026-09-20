package com.wapo.flagship.features.grid.model

data class Immersion(
        val mediaId: String? = null,
        val streamUrl: String? = null,
        val duration: Long? = null,
        val shouldShowSubscribe: Boolean? = null,
        val position: String? = null,
        val slug: String? = null,
        val showScrubber: Boolean? = null,
        val displayDate: String? = null,
        val displayLabel: String? = null,
        val displayTransparency: String? = null,
        val titlePrefix: String? = null,
        val title: String? = null,
        val coverImage: String? = null,
        val subscriptionLinks: SubscriptionLinks? = null,
        val tracking: AudioTracking? = null,
        val inlinePlayer: InlinePlayer? = null
)