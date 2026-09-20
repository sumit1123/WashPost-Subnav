package com.wapo.flagship.features.articles2.models.deserialized

enum class Style(
    val value: String,
) {
    BRIEFS("briefs"),
    OPINIONS("opinions"),
    SEVEN_LIVE("the-7-live"),
    DEFAULT(""),
    ;

    companion object {
        fun getValue(input: String?): Style {
            for (b in values()) {
                if (b.value.equals(input, ignoreCase = true)) {
                    return b
                }
            }
            return DEFAULT
        }
    }
}
