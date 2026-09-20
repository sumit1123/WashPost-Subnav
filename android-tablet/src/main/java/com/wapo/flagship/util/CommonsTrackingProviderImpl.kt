/*
 * Copyright (c) 2023. The Washington Post
 */
package com.wapo.flagship.util

import com.squareup.moshi.Moshi
import com.wapo.android.commons.util.CommonsTrackingProvider
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.articles2.models.OmnitureXJsonAdapter
import com.wapo.flagship.util.tracking.Measurement

class CommonsTrackingProviderImpl : CommonsTrackingProvider {
    override fun trackGiftSendClicked(
        contentUrl: String?,
        omnitureXJson: String?,
        selectedAppPackage: String,
        appSection: String?,
        isActionButton: Boolean,
        tabName: String?,
    ) {
        var omnitureX: OmnitureX? = null
        if (omnitureXJson != null) {
            val moshi = Moshi.Builder().build()
            omnitureX = OmnitureXJsonAdapter(moshi).fromJson(omnitureXJson)
        }
        val regex = Regex("\\{(.*)/")
        val shareAppChosen = regex.find(selectedAppPackage)?.groupValues?.get(1)

        if (tabName != null) {
            Measurement.setTabName(Measurement.getDefaultMap(), tabName)
        }
        Measurement.trackGiftSendClicked(
            contentUrl,
            omnitureX,
            "gift_share_$shareAppChosen",
            appSection,
            isActionButton,
        )
    }

    override fun trackVideoShare(
        sharedUrl: String?,
        socialName: String?,
        title: String?,
        isPushOriginated: Boolean,
        socialShareName: String?,
        tabName: String?,
    ) {
        if (tabName != null) {
            Measurement.setTabName(Measurement.getDefaultMap(), tabName)
        }
        Measurement.trackVerticalVideoShare(
            sharedUrl,
            socialName,
            title,
            isPushOriginated,
            socialShareName,
        )
    }

    override fun trackShare(
        sharedUrl: String?,
        socialName: String?,
        title: String?,
        isPushOriginated: Boolean,
        arcId: String?,
        appSection: String?,
        isActionButton: Boolean,
        tabName: String?,
    ) {
        if (tabName != null) {
            Measurement.setTabName(Measurement.getDefaultMap(), tabName)
        }
        Measurement.trackShare(
            sharedUrl,
            socialName,
            title,
            isPushOriginated,
            arcId,
            appSection,
            isActionButton,
            false,
            false,
        )
    }
}
