package com.wapo.flagship.features.grid.model

data class Headline(
    val text: String,
    val size: Size,
    val alignment: Alignment?,
    val fontStyle: FontStyle?,
    val type: BulletType? = null,
    val style: Style?,
    val prefixIcon: HeadlineIcon? = null,
)

enum class PagebuilderSize {
    XXSMALL,
    XSMALL,
    SMALL,
    NORMAL,
    LARGE,
    XLARGE,
    XXLARGE,
    HUGE,
    XHUGE,
    XXHUGE,
    GARGANTUAN,
    XGARGANTUAN,
    XXGARGANTUAN
}

enum class Size(val sp: Int) {
    TINY(16),
    XSMALL(20),
    SMALL(20),
    MEDIUM(20),
    STANDARD(26),
    LARGE(26),
    XLARGE(26),
    HUGE(42),
    MASSIVE(56),
    COLOSSAL(42),
    JUMBO(42),
    GARGANTUAN(42),
    COLOSSAL_ALL_CAPS(56),
    JUMBO_ALL_CAPS(56),
    GARGANTUAN_ALL_CAPS(56)
}

enum class Alignment {
    CENTER,
    LEFT,
    RIGHT,
    INHERIT
}

enum class FontStyle {
    HIGHLIGHT_STYLE,
    NORMAL_STYLE,
    THIN_STYLE,
    REGULAR_STYLE,
    BOLD_STYLE,
    ITALIC_STYLE,
    LIGHT_STYLE,
    ULTRA_STYLE,
    ULTRA_ITALIC_STYLE
}

enum class BulletType {
    NORMAL,
    BULLET
}

enum class Style {
    STYLE,
    CONVERSATIONS
}

enum class HeadlineIcon {
    LOGO,
    RIPPLE
}