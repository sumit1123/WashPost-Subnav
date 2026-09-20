/*
 * Copyright (c) 2019. The Washington Post
 */
package com.wapo.android.commons.util

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

@AndroidEntryPoint
class ShareBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var commonsTrackingProvider: CommonsTrackingProvider

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val selectedAppPackage: String = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 -> {
                    intent.getParcelableExtra<ComponentName>(Intent.EXTRA_CHOSEN_COMPONENT).let { comp ->
                        comp?.packageName?.let { comp.toString() } ?: "unknown"
                    }
                }
                else -> "unknown"
            }

            val socialShareName: String = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 -> {
                    intent.getParcelableExtra<ComponentName>(Intent.EXTRA_CHOSEN_COMPONENT).let { comp ->
                        comp?.packageName?.let { comp.toString() } ?: "unknown"
                    }
                }
                else -> "unknown"
            }

            val sharedUrl = intent.getStringExtra(ShareUtils.URL)
            val articleUrl: String? = intent.getStringExtra(ShareUtils.ARTICLE_URL)
            val title = intent.getStringExtra(ShareUtils.TITLE)
            val arcId = intent.getStringExtra(ShareUtils.ARC_ID)
            val tabName: String? = intent.getStringExtra(ShareUtils.TAB_NAME)
            val appSection = intent.getStringExtra(ShareUtils.APP_SECTION)
            val isSharingFromPush = intent.getBooleanExtra(ShareUtils.IS_SHARING_FROM_PUSH, false)
            val isGift = intent.getBooleanExtra(ShareUtils.IS_GIFT, false)
            val isVideo = intent.getBooleanExtra(ShareUtils.IS_VIDEO, false)
            val isActionButton = intent.getBooleanExtra(ShareUtils.IS_ACTION_BUTTON, false)
            val omnitureXJson: String? = intent.getStringExtra(ShareUtils.TRACKING_INFO)

            when {
                isGift -> commonsTrackingProvider.trackGiftSendClicked(articleUrl, omnitureXJson, selectedAppPackage, appSection, isActionButton, tabName)
                isVideo -> commonsTrackingProvider.trackVideoShare(sharedUrl, selectedAppPackage, title, isSharingFromPush, socialShareName, tabName)
                else -> commonsTrackingProvider.trackShare(sharedUrl, selectedAppPackage, title, isSharingFromPush, arcId, appSection, isActionButton, tabName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}