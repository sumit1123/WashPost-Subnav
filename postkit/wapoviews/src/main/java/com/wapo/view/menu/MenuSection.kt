package com.wapo.view.menu

class MenuSection(
        val displayName: String,
        val type: String,
        val databaseId: String,
        val bundleName: String,
        val sectionInfo: Array<MenuSection>
) {
    val isBlog: Boolean
        get() = BLOG_TYPE == type

    val isSection: Boolean
        get() = SECTION_TYPE == type

    companion object {
        @JvmField val BLOG_TYPE = "blog"
        @JvmField val SECTION_TYPE = "section"
        @JvmField val COMICS_TYPE = "comics"
        @JvmField val LABEL_TYPE = "label"
    }
}
