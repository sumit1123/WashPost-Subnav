package com.wapo.flagship.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import com.wapo.android.commons.util.Logger
import androidx.annotation.NonNull
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.articles2.models.OmnitureXJsonAdapter
import com.wapo.flagship.receivers.ShareBroadcastReceiver
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class Share private constructor(
    private val arcId: String?,
    private val byline: String?,
    private val fromPush: Boolean? = false,
    private val headline: String?,
    private val sectionLabel: String?,
    private val shareUrl: String?,
    private val articleUrl: String?,
    private val title: String?,
    private val text: String?,
    private val transparencyLabel: String?,
    private val isGiftShare: Boolean = false,
    private val isVerticalVideoShare: Boolean = false,
    private val isActionButton: Boolean = false,
    private val appSection: String? = null,
    private val omnitureXJson: String? = null,
    private val isVideoShare: Boolean = false,
    private val base64Image: String? = null,
    private val isSourceIAM: Boolean? = false,
) {
    fun shareItem(
        @NonNull context: Context,
    ) {
        val shareUrlFormatted = ConfigManager.getInstance().config.getShareUrl(shareUrl)
        val receiver = Intent(context, ShareBroadcastReceiver::class.java)
        receiver.putExtra(URL, shareUrlFormatted)
        receiver.putExtra(ARTICLE_URL, articleUrl)
        receiver.putExtra(TITLE, headline)
        receiver.putExtra(TEXT, text)
        receiver.putExtra(ARC_ID, arcId)
        receiver.putExtra(APP_SECTION, appSection)
        receiver.putExtra(IS_SHARING_FROM_PUSH, fromPush)
        receiver.putExtra(IS_GIFT, isGiftShare)
        receiver.putExtra(IS_VERTICAL_VIDEO, isVerticalVideoShare)
        receiver.putExtra(IS_ACTION_BUTTON, isActionButton)
        receiver.putExtra(TRACKING_INFO, omnitureXJson)
        receiver.putExtra(IS_VIDEO_SHARE, isVideoShare)
        receiver.putExtra(BASE64_IMAGE, base64Image)
        receiver.putExtra(IS_SOURCE_IAM, isSourceIAM)
        share(context, receiver)
    }

    fun share(
        @NonNull context: Context,
        shareReceiver: Intent? = null,
    ) {
        Logger.d(
            TAG,
            "arcId=$arcId, byline=$byline, fromPush=$fromPush, headline=$headline, sectionLabel=$sectionLabel, " +
                    "shareUrl=$shareUrl, transparencyLabel=$transparencyLabel",
        )

        // Subject - https://arcpublishing.atlassian.net/browse/AWA-3557
        var label =
            ALLOW_LISTED_SECTION_LABELS[
                ALLOW_LISTED_SECTION_LABELS.keys.firstOrNull { key ->
                    sectionLabel?.lowercase()?.contains(key) ?: false
                },
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
        var wpSubject =
            String.format(
                context.resources.getString(R.string.share_email_subject),
                subject,
            )

        // Body
        var url = shareUrl
        var plainBodyText =
            StringBuilder()
                .apply {
                    if (!text.isNullOrEmpty()) {
                        append(text).append(STRING_NEW_LINE)
                    } else if (!subject.isNullOrEmpty()) {
                        append(subject).append(STRING_NEW_LINE)
                    }
                    if (!text.isNullOrEmpty() || !subject.isNullOrEmpty() || !byline.isNullOrEmpty()) {
                        append(STRING_NEW_LINE)
                    }
                    if (!url.isNullOrEmpty()) {
                        append(url).append(STRING_NEW_LINE)
                    }
                }.toString()
        Logger.d(TAG, "plainBodyText=$plainBodyText")

        // Intent
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_SUBJECT, wpSubject)
                putExtra(Intent.EXTRA_TEXT, plainBodyText)
                type = "text/plain"
            }

        base64Image?.let {
            var bitmap: Bitmap? = null
            try {
                val byteArray = Base64.decode(base64Image, Base64.DEFAULT)
                bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } catch (e: Exception) {
                Logger.e(TAG, "Base64 decoding failed: $e")
            }

            if (bitmap != null) {
                try {
                    val imagePath = File(context.cacheDir, "images")
                    imagePath.mkdir()
                    val stream =
                        FileOutputStream("$imagePath/shareimage.png") // overwrite this image every time
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.close()
                    val newFile = File(imagePath, "shareimage.png")
                    val cacheProvider = "${BuildConfig.APPLICATION_ID}.cacheprovider"
                    val contentUri = FileProvider.getUriForFile(context, cacheProvider, newFile)
                    shareReceiver?.putExtra(
                        BASE64_IMAGE,
                        contentUri
                    ) // Avoids "!!! FAILED BINDER TRANSACTION !!!" error on "getBroadcast()" call below
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    shareIntent.type = "image/jpeg"
                } catch (e: IOException) {
                    Logger.e(TAG, "Unable to share image: $e")
                }
            }
        }

        val openInChooser =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && shareReceiver != null) {
                val flags =
                    if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.S
                    ) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
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
        private var text: String? = null
        private var transparencyLabel: String? = null
        private var articleUrl: String? = null
        private var appSection: String? = null
        private var isGiftShare: Boolean = false
        private var isVerticalVideoShare: Boolean = false
        private var isActionButton: Boolean = false
        private var omnitureXJson: String? = null
        private var isVideoShare: Boolean = false
        private var base64Image: String? = null
        private var isSourceIAM: Boolean = false

        fun arcId(id: String?): Builder = this.apply { this.arcId = id }

        fun byline(byline: String?): Builder = this.apply { this.byline = byline }

        fun fromPush(fromPush: Boolean?): Builder = this.apply { this.fromPush = fromPush ?: false }

        fun headline(headline: String?): Builder = this.apply { this.headline = headline }

        fun sectionLabel(label: String?): Builder = this.apply { this.sectionLabel = label }

        fun shareUrl(url: String?): Builder = this.apply { this.shareUrl = url }

        fun articleUrl(url: String?): Builder = this.apply { this.articleUrl = url }

        fun title(title: String?): Builder = this.apply { this.title = title }

        fun text(text: String?): Builder = this.apply { this.text = text }

        fun transparencyLabel(label: String?): Builder =
            this.apply { this.transparencyLabel = label }

        fun trackingInfo(trackingInfo: OmnitureX?): Builder =
            this.apply {
                val moshi = Moshi.Builder().build()
                trackingInfo?.let {
                    this.omnitureXJson = OmnitureXJsonAdapter(moshi).toJson(it)
                }
            }

        fun isGiftShare(isGift: Boolean): Builder = this.apply { this.isGiftShare = isGift }

        fun isVerticalVideoShare(isVideo: Boolean): Builder =
            this.apply { this.isVerticalVideoShare = isVideo }

        fun isActionButton(isActionButton: Boolean): Builder =
            this.apply { this.isActionButton = isActionButton }

        fun appSection(appSection: String?): Builder = this.apply { this.appSection = appSection }

        fun isVideoShare(isVideo: Boolean): Builder = this.apply { this.isVideoShare = isVideo }

        fun base64Image(base64Image: String?): Builder =
            this.apply { this.base64Image = base64Image }

        fun isSourceIAM(isSourceIAM: Boolean?): Builder =
            this.apply { this.isSourceIAM = isSourceIAM ?: false }

        fun build(): Share =
            Share(
                arcId,
                byline,
                fromPush,
                headline,
                sectionLabel,
                shareUrl,
                articleUrl,
                title,
                text,
                transparencyLabel,
                isGiftShare,
                isVerticalVideoShare,
                isActionButton,
                appSection,
                omnitureXJson,
                isVideoShare,
                base64Image,
                isSourceIAM,
            )
    }

    companion object {
        private const val TAG = "Share"
        private const val STRING_NEW_LINE = "\n"
        private val ALLOW_LISTED_SECTION_LABELS =
            mapOf(
                "opinion" to "Opinion",
                "opinión" to "Opinion",
            )
        private val DENY_LISTED_TRANSPARENCY_LABELS = listOf("news")
        const val URL = "shared.url"
        const val ARTICLE_URL = "shared.articleUrl"
        const val TITLE = "shared.title"
        const val TEXT = "shared.text"
        const val ARC_ID = "shared.arcId"
        const val APP_SECTION = "shared.appSection"
        const val IS_SHARING_FROM_PUSH = "shared.fromPush"
        const val IS_GIFT = "shared.isGift"
        const val IS_VERTICAL_VIDEO = "shared.isVerticalVideo"
        const val IS_ACTION_BUTTON = "shared.isActionButton"
        const val TRACKING_INFO = "shared.trackingInfo"
        const val IS_VIDEO_SHARE = "shared.isVideoShare"
        const val BASE64_IMAGE = "shared.base64Image"
        const val IS_SOURCE_IAM = "source.iam"
    }
}
