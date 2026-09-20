package com.wapo.flagship.features.grid.model

data class CompoundLabel(
    val text: String?,
    var secondaryText: String?,
    var alignment: Alignment?,
    var link: Link?,
    val hasArrow: Boolean?,
    val type: Type,
    val style: LabelStyle?,
    val position: LabelPosition,
    val icon: Icon?,
    var isStoryLabel: Boolean = false,
    val form: Form?
) {
    enum class Type {
        FullSpan,
        Package,
        Pill,
        MiniAllCaps,
        Kicker,
        LiveUpdates,
        Exclusive,
        PackageNested,
        Promo,
        Cta,
        Newsletter,
        Button,
        Comment,
        BrandPromo,
    }

    enum class LabelPosition {
        Default,
        AboveHeadline
    }

    enum class Icon(val isColored: Boolean) {
        CAMERA(false),
        CHART(false),
        HEADPHONES(false),
        ELECTION_STAR(true),
        PLAY(false),
        OLYMPICS(true),
        THE_7(true),
        THE_7_LIVE_CHIP(true),
        WORLD_CUP(true),
        POST_PULSE(true),
        COMMENTS(false),
        EXTERNAL_LINK(false)
    }

    enum class LabelStyle {
        OPINIONS,
        WP_INTELLIGENCE,
        THE_SEVEN_LIVE,
    }
}