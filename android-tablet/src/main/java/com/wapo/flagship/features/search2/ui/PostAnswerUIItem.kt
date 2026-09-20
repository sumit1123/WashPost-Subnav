package com.wapo.flagship.features.search2.ui

import com.wapo.flagship.features.search2.model.Citation
import com.wapo.flagship.features.search2.model.ParsedTextResult

sealed class PostAnswerUIItem {
    data class TextItem(
        val subtype: TextSubtype?,
        val content: String,
        val mimeType: MimeType?,
        val streamingUrl: String? = null,
        val icon: String? = null,
        val bottomSheetInfo: BottomSheetInfo? = null,
        val parsed: ParsedTextResult? = null,
        val citations: List<Citation>? = null,
    ) : PostAnswerUIItem()

    data class Carousel(
        val subtype: CarouselSubtype?,
        val items: List<CarouselUIItem>,
    ) : PostAnswerUIItem()

    data class Feedback(
        val endPointUrl: String,
        val responseId: String,
    ) : PostAnswerUIItem()
}

/** FYI this won't actually be used until we fully implement https://arcpublishing.atlassian.net/browse/AWA-10262 **/
data class BottomSheetInfo(
    val heading: String?,
    val content: String?,
)

data class CarouselUIItem(
    val id: Int,
    val url: String,
    val publishDateMillis: Long,
    val content: String,
    val imageUrl: String,
    val passages: List<String>?,
)

enum class TextSubtype(
    val value: String,
) {
    TITLE("title"),
    SUBTITLE("subtitle"),
    DESCRIPTION("description"),
    BODY("body"),
}

enum class CarouselSubtype(
    val value: String,
) {
    PASSAGES("passages"),
}

enum class MimeType(
    val value: String,
) {
    HTML("text/html"),
    PLAIN("text/plain"),
}

enum class WidthFactor(
    val value: String,
) {
    DEFAULT("default"),
    FULL_BLEED("full_bleed"),
}
