/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.model

import com.google.gson.annotations.SerializedName

data class Arrangements(
        val default: DefaultArrangement?,
        val left: Zone?,
        val right: Zone?,
        val top: Zone?,
        val bottom: Zone?,
        val sidebar: Zone?
)

class DefaultArrangement(
        val left: Zone?,
        val right: Zone?,
        val top: Zone?,
        val bottom: Zone?,
        val sidebar: Zone?
)
class Zone(
        val items: MutableList<SubItemType>?,
        val width: ArtWidth?,
        val valign: VerticalAlignment?
)

enum class SubItemType {
    HEADLINE,
    MEDIA,
    SLIDESHOW,
    BYLINE,
    BLURB,
    LABEL,
    LIVE_TICKER,
    RELATED_LINKS,
    AUDIO,
    OLYMPICS_MEDALS,
    CTA,
    FOOT_NOTE,
    AUDIO_ARTICLE,
    TOPPER_LABEL,
    WEB_EMBED,
    COUNT
}

enum class VerticalAlignment {
    CENTER,
    BOTTOM
}