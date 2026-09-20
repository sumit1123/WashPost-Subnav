package com.washingtonpost.android.save.models

import com.wapo.android.commons.iterable.AttributionInfo
import com.washingtonpost.android.save.types.MyPostSection
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem

sealed class PreviewItem {
    class SectionPreviewItem(
        val myPostSection: MyPostSection,
        val articleList: List<MyPostArticleItem>?,
        val carouselList: List<CarouselViewItem>?,
        val authors: List<AuthorEntity>? = null,
        val topicsList: List<MyPostTopicItem>? = null
    ) : PreviewItem()

    class FooterPreviewItem : PreviewItem()

    class EmptyPreviewItem(val myPostSection: MyPostSection, val emptyState: EmptyState) :
        PreviewItem()

    class BannerPreviewItem(
        val attributionInfo: AttributionInfo? = null,
        val title: String,
        val url: String?,
        val subtitle: String,
        val imageUrl: String,
        val darkImageUrl: String?,
        val asset: String? = null,
        val assetHasPriority: Boolean? = null,
        val minimizeOnNarrowScreen: Boolean? = null,
        val buttonText: String? = null,
    ) : PreviewItem()

    class BirthdayFrontPagePreviewItem: PreviewItem()

    override fun equals(other: Any?): Boolean {
        return if (other as? PreviewItem == null) {
            false
        } else if (other === this) {
            true
        } else if (other is SectionPreviewItem && this is SectionPreviewItem) {
            val isSectionEqual = (other.myPostSection == myPostSection)
            val areItemsEqual = (articleList == null && other.articleList == null)
                    || (articleList != null && other.articleList != null
                    && articleList.size == other.articleList.size
                    && articleList.zip(other.articleList)
                .all { (i1, i2) -> i1.contentUrl == i2.contentUrl })
            val areAuthorsEqual = (authors == null && other.authors == null)
                    || (authors != null && other.authors != null
                    && authors.size == other.authors.size
                    && authors.zip(other.authors)
                .all { (i1, i2) -> i1.authorId == i2.authorId })
            val areTopicsEqual = (topicsList == other.topicsList)
            isSectionEqual and areItemsEqual and areAuthorsEqual and areTopicsEqual
        } else if (other is EmptyPreviewItem && this is EmptyPreviewItem) {
            other.emptyState == this.emptyState
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return (super.hashCode() * 31)
    }
}
