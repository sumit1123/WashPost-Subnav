package com.washingtonpost.android.save.models

import com.washingtonpost.android.save.types.MyPostSection

sealed class DetailItem(open val myPostSection: MyPostSection) {
    class Header(override val myPostSection: MyPostSection) : DetailItem(myPostSection)

    class Article(override val myPostSection: MyPostSection, val articleItem: MyPostArticleItem) :
        DetailItem(myPostSection)

    class FooterArchive(override val myPostSection: MyPostSection) : DetailItem(myPostSection)

    class Empty(override val myPostSection: MyPostSection, val emptyState: EmptyState) :
        DetailItem(myPostSection)

    override fun equals(other: Any?): Boolean {
        return if (other as? DetailItem == null) {
            false
        } else if (other === this) {
            true
        } else if (other is Article && this is Article) {
            val isSectionEqual = other.myPostSection == myPostSection
            val areItemsEqual = other.articleItem.contentUrl == articleItem.contentUrl
            isSectionEqual and areItemsEqual
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return (super.hashCode() * 31)
    }
}
