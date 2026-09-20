/*
 * Copyright (c) 2019. The Washington Post
 */
package com.wapo.flagship.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.articles2.models.OmnitureXJsonAdapter
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.tracking.Measurement

/**
 * Created by adkinsj on 2019-07-22.
 */
class ShareBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        try {
            val selectedAppPackage: String =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 -> {
                        intent.getParcelableExtra<ComponentName>(Intent.EXTRA_CHOSEN_COMPONENT).let { comp ->
                            comp?.packageName?.let { comp.toString() } ?: "unknown"
                        }
                    }
                    else -> "unknown"
                }

            val socialShareName: String =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 -> {
                        intent.getParcelableExtra<ComponentName>(Intent.EXTRA_CHOSEN_COMPONENT).let { comp ->
                            comp?.packageName?.let { comp.toString() } ?: "unknown"
                        }
                    }
                    else -> "unknown"
                }

            val sharedUrl = intent.getStringExtra(Share.URL)
            val articleUrl: String? = intent.getStringExtra(Share.ARTICLE_URL)
            val title = intent.getStringExtra(Share.TITLE)
            val arcId = intent.getStringExtra(Share.ARC_ID)
            val appSection = intent.getStringExtra(Share.APP_SECTION)
            val isSharingFromPush = intent.getBooleanExtra(Share.IS_SHARING_FROM_PUSH, false)
            val isGift = intent.getBooleanExtra(Share.IS_GIFT, false)
            val isVerticalVideo = intent.getBooleanExtra(Share.IS_VERTICAL_VIDEO, false)
            val isActionButton = intent.getBooleanExtra(Share.IS_ACTION_BUTTON, false)
            val omnitureXJson: String? = intent.getStringExtra(Share.TRACKING_INFO)
            val isVideoShare = intent.getBooleanExtra(Share.IS_VIDEO_SHARE, false)
            val isSourceIAM = intent.getBooleanExtra(Share.IS_SOURCE_IAM, false)

            if (isGift) {
                var omnitureX: OmnitureX? = null
                if (omnitureXJson != null) {
                    val moshi = Moshi.Builder().build()
                    omnitureX = OmnitureXJsonAdapter(moshi).fromJson(omnitureXJson)
                }
                val regex = Regex("\\{(.*)/")
                val shareAppChosen = regex.find(selectedAppPackage)?.groupValues?.get(1)
                Measurement.trackGiftSendClicked(
                    articleUrl,
                    omnitureX,
                    "gift_share_$shareAppChosen",
                    appSection,
                    isActionButton,
                )
            } else if (isVerticalVideo) {
                Measurement.trackVerticalVideoShare(
                    sharedUrl,
                    selectedAppPackage,
                    title,
                    isSharingFromPush,
                    socialShareName,
                )
            } else {
                Measurement.trackShare(
                    sharedUrl,
                    selectedAppPackage,
                    title,
                    isSharingFromPush,
                    arcId,
                    appSection,
                    isActionButton,
                    isVideoShare,
                    isSourceIAM,
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
