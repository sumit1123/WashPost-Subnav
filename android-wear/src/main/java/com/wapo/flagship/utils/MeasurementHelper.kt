/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

@file:JvmName("MeasurementHelper")

package com.wapo.flagship.utils

import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.json.TrackingInfo

fun OmnitureX.toTrackingInfo(): TrackingInfo {
    return TrackingInfo().also {
        it.arcId = arcId
        it.authorId = authorId
        //it.authorType = authorType
        it.channel = channel
        it.contentAuthor = contentAuthor
        it.contentId = contentId
        it.contentSource = contentSource
        it.contentTopics = contentTopics
        it.contentType = contentType
        it.newsroomDesk = newsroomDesk
        it.newsroomSubdesk = newsroomSubdesk
        it.subSection = subSection

        it.pageName = pageName
        it.pageNumber = ""
        it.contentSubsection = ""
        it.searchKeywords = ""
        it.pageFormat = ""
        it.blogName = ""
        it.contentURL = ""
        it.interfaceType = ""
        it.source = contentSource
        it.primarySection = ""
        it.secondarySection = ""
        it.title = title
        //it.firstPublished = null
        it.trackingTags = trackingTags
    }
}