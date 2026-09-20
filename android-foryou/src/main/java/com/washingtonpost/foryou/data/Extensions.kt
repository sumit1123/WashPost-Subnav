package com.washingtonpost.foryou.data

val showAuthorImageInSections = listOf("Opinion", "Perspective", "Review", "Analysis", "Advice")
fun RecommendationsItem.byline(): String {
    var byline = ""
    val filteredCredits =
        credits?.by?.filter { !it.name.isNullOrEmpty() || !it.additionalProperties?.original?.byline.isNullOrEmpty() }
    if (filteredCredits?.isNotEmpty() == true) {
        byline = "By "
        filteredCredits.withIndex().forEach {
            byline += "${it.value.name ?: it.value.additionalProperties?.original?.byline}"
            if (filteredCredits.size > 1 && it.index != filteredCredits.size - 1) {
                byline += ", "
            }
        }
    }
    return byline
}

fun RecommendationsItem.getURL(): String {
    return "https://www.washingtonpost.com$url"
}

fun RecommendationsItem.getVideoShareUrl(): String {
    return "https://www.washingtonpost.com$canonicalUrl"
}

fun RecommendationsItem.hasAuthorImage(): Boolean {
    return (credits?.by?.size == 1) && !credits.by[0].image?.url.isNullOrEmpty() && (showAuthorImageInSections.contains(
        label?.basic?.text
    ) || showAuthorImageInSections.contains(label?.transparency?.text))
}

fun RecommendationsItem.getImage(): String? {
    return promoItems?.basic?.additionalProperties?.sizeNormalizedUrl.orEmpty().ifEmpty { promoItems?.basic?.url }
}

fun RecommendationsItem.getDate(): Long?{
    return displayDate?.time ?: publishDate?.time ?: firstPublishDate?.time
}

