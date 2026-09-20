package com.wapo.flagship.features.articles2.models.recirculation

import com.washingtonpost.android.recirculation.carousel.models.CarouselArticleItem

object AutoRecirculationMapper {
    fun AutoRecircArticle.toCarouselViewItem(collection: AutoRecircCollection): CarouselArticleItem {
        return CarouselArticleItem(
            arcId = this.arcId.orEmpty(),
            contentUrl = contentUrl(),
            kicker = label?.transparency?.text,
            storyType = label?.basic?.text,
            label = label?.basic?.text.orEmpty(),
            title = headlines?.basic.orEmpty(),
            byline = byline(),
            imageUrl = promoItems?.basic?.url,
            sectionName = collection.title.orEmpty(),
            displayDate = displayDate.toString(),
        )
    }

    private fun AutoRecircArticle.contentUrl(): String {
        val canonicalPath = canonicalUrl
        return if (canonicalPath.isNullOrBlank()) {
            ""
        } else {
            "https://www.washingtonpost.com$canonicalPath"
        }
    }

    private fun AutoRecircArticle.byline(): String {
        val byNames = credits?.by?.joinToString(",") { it.name.orEmpty() }
        return if (!byNames.isNullOrEmpty()) {
            "By $byNames"
        } else {
            ""
        }
    }

    private fun Credits.toUiModel(): com.washingtonpost.foryou.data.Credits {
        return com.washingtonpost.foryou.data.Credits(
            by = by?.map {
                com.washingtonpost.foryou.data.ByItem(
                    image = null,
                    Id = it.id,
                    name = it.name,
                    additionalProperties = it.additionalProperties?.toUiModel(),
                )
            }
        )
    }

    private fun Label.toUiModel(): com.washingtonpost.foryou.data.Label {
        return com.washingtonpost.foryou.data.Label(
            transparency = transparency?.let {
                com.washingtonpost.foryou.data.Transparency(
                    display = it.display,
                    text = it.text,
                    url = it.url,
                )
            },
            basic = basic?.let {
                com.washingtonpost.foryou.data.Basic(
                    aspectRatio = null,
                    creditsDisplay = null,
                    width = null,
                    additionalProperties = it.additionalProperties?.toUiModel(),
                    creditsCaptionDisplay = null,
                    url = it.url,
                    height = null,
                    text = it.text,
                    style = null,
                    headlinePrefix = null,
                )
            },
        )
    }

    private fun AdditionalProperties.toUiModel(): com.washingtonpost.foryou.data.AdditionalProperties {
        return com.washingtonpost.foryou.data.AdditionalProperties(
            sizeNormalizedUrl = sizeNormalizedUrl,
            original = null,
            audioArticle = null,
        )
    }
}
