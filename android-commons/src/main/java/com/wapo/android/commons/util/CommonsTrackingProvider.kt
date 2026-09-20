/*
 * Copyright (c) 2023. The Washington Post
 */
package com.wapo.android.commons.util

interface CommonsTrackingProvider {

    fun trackGiftSendClicked(contentUrl: String?,
                             omnitureXJson: String?,
                             selectedAppPackage: String,
                             appSection: String?,
                             isActionButton: Boolean,
                             tabName: String?
    )

    fun trackVideoShare(sharedUrl: String?,
                        socialName: String?,
                        title: String?,
                        isPushOriginated: Boolean,
                        socialShareName: String?,
                        tabName: String?
    )

    fun trackShare(sharedUrl: String?,
                   socialName: String?,
                   title: String?,
                   isPushOriginated: Boolean,
                   arcId: String?,
                   appSection: String?,
                   isActionButton: Boolean,
                   tabName: String?
    )
}