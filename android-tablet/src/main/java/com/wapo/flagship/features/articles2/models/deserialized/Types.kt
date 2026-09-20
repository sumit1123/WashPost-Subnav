package com.wapo.flagship.features.articles2.models.deserialized

enum class Types(
    var type: String,
) {
    RECIRC("recirc"),
    TAGLINE("tagline"),
}

enum class Card(
    var card: String,
) {
    NO_CARD("no_card"),
    FIRST_CARD_START("first_card_start"),
    LAST_CARD_END("last_card_end"),
    CARD_START("card_start"),
    CARD_MIDDLE("card_middle"),
    CARD_END("card_end"),
}
