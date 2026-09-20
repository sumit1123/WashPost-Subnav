package com.washingtonpost.android.follow.misc

data class FollowTracking(
        @JvmField var pageName: String? = "",
        @JvmField var channel: String? = "",
        @JvmField var contentAuthor: String? = "",
        @JvmField var authorId: String? = "",
        @JvmField var miscellany: String? = "",
        @JvmField var tabName: String? = "",
        @JvmField var appSection: String? = "",
        @JvmField var contentSection: String? = "",
        @JvmField var contentSubsection: String? = "")