package com.wapo.android.commons.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.NonNull
import com.wapo.android.commons.logger.R
import java.lang.StringBuilder

data class ShareUtils private constructor(private val arcId: String?,
                                          private val byline: String?,
                                          private val fromPush: Boolean? = false,
                                          private val headline: String?,
                                          private val sectionLabel: String?,
                                          private val shareUrl: String?,
                                          private val articleUrl: String?,
                                          private val title: String?,
                                          private val transparencyLabel: String?,
                                          private val tabName: String? = null,
                                          private val appSection: String? = null,
                                          private val isGiftShare: Boolean = false,
                                          private val isVideoShare: Boolean = false,
                                          private val isActionButton: Boolean = false,
                                          private val omnitureXJson: String? = null
) {

    fun shareItem(@NonNull context: Context) {
        val shareUrlFormatted = Utils.getSharedUrl(shareUrl)
        val receiver = Intent(context, ShareBroadcastReceiver::class.java)
        receiver.putExtra(ARC_ID, arcId)
        receiver.putExtra(IS_SHARING_FROM_PUSH, fromPush)
        receiver.putExtra(TITLE, headline)
        receiver.putExtra(URL, shareUrlFormatted)
        receiver.putExtra(ARTICLE_URL, articleUrl)
        receiver.putExtra(TAB_NAME, tabName)
        receiver.putExtra(APP_SECTION, appSection)
        receiver.putExtra(IS_GIFT, isGiftShare)
        receiver.putExtra(IS_VIDEO, isVideoShare)
        receiver.putExtra(IS_ACTION_BUTTON, isActionButton)
        receiver.putExtra(TRACKING_INFO, omnitureXJson)
        share(context, receiver)
    }

    fun share(@NonNull context: Context, shareReceiver: Intent? = null) {
        Logger.d(TAG, "arcId=$arcId, byline=$byline, fromPush=$fromPush, headline=$headline, sectionLabel=$sectionLabel, " +
                "shareUrl=$shareUrl, transparencyLabel=$transparencyLabel")

        // Subject - https://arcpublishing.atlassian.net/browse/AWA-3557
        var label = ALLOW_LISTED_SECTION_LABELS[
                ALLOW_LISTED_SECTION_LABELS.keys.firstOrNull { key ->
                    sectionLabel?.lowercase()?.contains(key) ?: false
                }
        ]
        if (label.isNullOrBlank()) {
            // look for transparency label
            if (!DENY_LISTED_TRANSPARENCY_LABELS.contains(transparencyLabel?.lowercase())) {
                label = transparencyLabel
            }
        }
        var subject =
                if (!label.isNullOrEmpty()) {
                    if (!headline.isNullOrEmpty()) {
                        "$label | $headline"
                    } else {
                        label
                    }
                } else if (!headline.isNullOrEmpty()) {
                    headline
                } else {
                    ""
                }
        var wpSubject = String.format(context.resources.getString(R.string.share_email_subject), subject)

        // Body
        var url = shareUrl
        var plainBodyText = StringBuilder().apply {
            if (!subject.isNullOrEmpty()) {
                append(subject).append(STRING_NEW_LINE)
            }
            if (!subject.isNullOrEmpty() || !byline.isNullOrEmpty()) {
                append(STRING_NEW_LINE)
            }
            if (!url.isNullOrEmpty()) {
                append(url).append(STRING_NEW_LINE).append(STRING_NEW_LINE)
            }
        }.toString()
        Logger.d(TAG, "plainBodyText=$plainBodyText")

        // Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, wpSubject)
            putExtra(Intent.EXTRA_TEXT, plainBodyText)
            type = "text/plain"
        }

        val openInChooser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && shareReceiver != null) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getBroadcast(context, 0, shareReceiver, flags)
            Intent.createChooser(shareIntent, title, pendingIntent.intentSender)
        } else {
            Intent.createChooser(shareIntent, title)
        }
        context.startActivity(openInChooser)
    }

    class Builder {
        private var arcId: String? = null
        private var byline: String? = null
        private var fromPush: Boolean = false
        private var headline: String? = null
        private var sectionLabel: String? = null
        private var shareUrl: String? = null
        private var title: String? = null
        private var transparencyLabel: String? = null
        private var articleUrl: String? = null
        private var tabName: String? = null
        private var appSection: String? = null
        private var isGiftShare: Boolean = false
        private var isVideoShare: Boolean = false
        private var isActionButton: Boolean = false
        private var omnitureXJson: String? = null

        fun arcId(id: String?): Builder {
            return this.apply { this.arcId = id }
        }

        fun byline(byline: String?): Builder {
            return this.apply { this.byline = byline }
        }

        fun fromPush(fromPush: Boolean?): Builder {
            return this.apply { this.fromPush = fromPush ?: false }
        }

        fun headline(headline: String?): Builder {
            return this.apply { this.headline = headline }
        }

        fun sectionLabel(label: String?): Builder {
            return this.apply { this.sectionLabel = label }
        }

        fun shareUrl(url: String?): Builder {
            return this.apply { this.shareUrl = url }
        }

        fun articleUrl(url: String?): Builder {
            return this.apply { this.articleUrl = url }
        }

        fun title(title: String?): Builder {
            return this.apply { this.title = title }
        }

        fun transparencyLabel(label: String?): Builder {
            return this.apply { this.transparencyLabel = label }
        }

        fun tabName(tabName: String?): Builder {
            return this.apply { this.tabName = tabName }
        }

        fun appSection(appSection: String?): Builder {
            return this.apply { this.appSection = appSection }
        }

        fun isGiftShare(isGiftShare: Boolean): Builder {
            return this.apply { this.isGiftShare = isGiftShare }
        }

        fun isVideoShare(isVideoShare: Boolean): Builder {
            return this.apply { this.isVideoShare = isVideoShare }
        }

        fun isActionButton(isActionButton: Boolean): Builder {
            return this.apply { this.isActionButton = isActionButton }
        }

        fun omnitureXJson(omnitureXJson: String?): Builder {
            return this.apply { this.omnitureXJson = omnitureXJson }
        }

        fun build(): ShareUtils {
            return ShareUtils(
                arcId,
                byline,
                fromPush,
                headline,
                sectionLabel,
                shareUrl,
                articleUrl,
                title,
                transparencyLabel,
                tabName,
                appSection,
                isGiftShare,
                isVideoShare,
                isActionButton
            )
        }
    }

    companion object {
        private const val TAG = "Share"
        private const val STRING_NEW_LINE = "\n"
        private val ALLOW_LISTED_SECTION_LABELS = mapOf("opinion" to "Opinion", "opinión" to "Opinion")
        private val DENY_LISTED_TRANSPARENCY_LABELS = listOf("news")
        const val URL = "shared.url"
        const val ARTICLE_URL = "shared.articleUrl"
        const val TITLE = "shared.title"
        const val ARC_ID = "shared.arcId"
        const val TAB_NAME = "shared.tabName"
        const val APP_SECTION = "shared.appSection"
        const val IS_SHARING_FROM_PUSH = "shared.fromPush"
        const val IS_GIFT = "shared.isGift"
        const val IS_VIDEO = "shared.isVideo"
        const val IS_ACTION_BUTTON = "shared.isActionButton"
        const val TRACKING_INFO = "shared.trackingInfo"
    }
}
