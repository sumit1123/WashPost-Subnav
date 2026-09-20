package com.wapo.flagship.features.deeplinks

import android.net.Uri
import android.text.TextUtils
import com.urbanairship.actions.DeepLinkAction
import com.urbanairship.iam.InAppMessage
import com.urbanairship.iam.InAppMessageExtender
import com.urbanairship.iam.TextInfo
import com.urbanairship.iam.banner.BannerDisplayContent
import com.urbanairship.iam.fullscreen.FullScreenDisplayContent
import com.urbanairship.iam.modal.ModalDisplayContent
import com.washingtonpost.android.paywall.util.PaywallUtil
import java.util.regex.Pattern

/**
 * Helper class to override text of any incoming airship in app message before it is displayed
 */
class AirshipInAppMessageExtender : InAppMessageExtender {
    override fun extend(message: InAppMessage): InAppMessage {
        val inAppMessageBuilder = InAppMessage.newBuilder(message)
        when (val displayContent = AirshipInAppMessageListener.getDisplayContent(message)) {
            is ModalDisplayContent -> {
                val modalDisplayContent = ModalDisplayContent.newBuilder(displayContent)
                val bodyTextInfo =
                    displayContent.body?.let {
                        if (!TextUtils.isEmpty(it.text)) {
                            TextInfo.newBuilder(
                                it,
                            )
                        } else {
                            null
                        }
                    }
                bodyTextInfo?.let {
                    // determine if this IAM is a subs offer and replace any placeholder text if necessary
                    extractProductAndOffer(message)?.let { productAndOffer ->
                        it.setText(
                            PaywallUtil.buildDynamicTextForProductAndOffer(
                                displayContent.body?.text,
                                productAndOffer,
                            ),
                        )
                    }
                }
                bodyTextInfo?.let {
                    modalDisplayContent.setBody(bodyTextInfo.build())
                }
                inAppMessageBuilder.setDisplayContent(modalDisplayContent.build())
            }
            is BannerDisplayContent -> {
                val bannerDisplayContent = BannerDisplayContent.newBuilder(displayContent)
                val bodyTextInfo =
                    displayContent.body?.let {
                        if (!TextUtils.isEmpty(it.text)) {
                            TextInfo.newBuilder(
                                it,
                            )
                        } else {
                            null
                        }
                    }
                bodyTextInfo?.let {
                    // determine if this IAM is a subs offer and replace any placeholder text if necessary
                    extractProductAndOffer(message)?.let { productAndOffer ->
                        it.setText(
                            PaywallUtil.buildDynamicTextForProductAndOffer(
                                displayContent.body?.text,
                                productAndOffer,
                            ),
                        )
                    }
                }
                bodyTextInfo?.let {
                    bannerDisplayContent.setBody(bodyTextInfo.build())
                }
                inAppMessageBuilder.setDisplayContent(bannerDisplayContent.build())
            }
            is FullScreenDisplayContent -> {
                val fullScreenDisplayContent = FullScreenDisplayContent.newBuilder(displayContent)
                val bodyTextInfo =
                    displayContent.body?.let {
                        if (!TextUtils.isEmpty(it.text)) {
                            TextInfo.newBuilder(
                                it,
                            )
                        } else {
                            null
                        }
                    }
                bodyTextInfo?.let {
                    // determine if this IAM is a subs offer and replace any placeholder text if necessary
                    extractProductAndOffer(message)?.let { productAndOffer ->
                        it.setText(
                            PaywallUtil.buildDynamicTextForProductAndOffer(
                                displayContent.body?.text,
                                productAndOffer,
                            ),
                        )
                    }
                }
                bodyTextInfo?.let {
                    fullScreenDisplayContent.setBody(bodyTextInfo.build())
                }
                inAppMessageBuilder.setDisplayContent(fullScreenDisplayContent.build())
            }
        }

        return inAppMessageBuilder.build()
    }

    private fun getButtonAction(message: InAppMessage): String? =
        message.run {
            when (val displayContent = AirshipInAppMessageListener.getDisplayContent(this)) {
                is ModalDisplayContent ->
                    displayContent.buttons
                        .firstOrNull()
                        ?.actions
                        ?.get(
                            DeepLinkAction.DEFAULT_REGISTRY_NAME,
                        )?.optString()
                is BannerDisplayContent ->
                    displayContent.buttons
                        .firstOrNull()
                        ?.actions
                        ?.get(
                            DeepLinkAction.DEFAULT_REGISTRY_NAME,
                        )?.optString()
                is FullScreenDisplayContent ->
                    displayContent.buttons
                        .firstOrNull()
                        ?.actions
                        ?.get(
                            DeepLinkAction.DEFAULT_REGISTRY_NAME,
                        )?.optString()
                else -> null
            }
        }

    /**
     * Identify button action and determine if the link is a subs offer link that matches
     * DeepLinksProcessor.DIRECT_IAP_PURCHASE_PATTERN or DeepLinksProcessor.DIRECT_IAP_PURCHASE_OFFER_PATTERN
     * If it is, extract product and offer (optional) from the path
     */
    private fun extractProductAndOffer(message: InAppMessage): Pair<String, String?>? {
        getButtonAction(message)?.let {
            val uri = Uri.parse(it)
            if (Pattern.matches(DeepLinksProcessor.DIRECT_IAP_PURCHASE_PATTERN, uri.path ?: "") ||
                Pattern.matches(
                    DeepLinksProcessor.DIRECT_IAP_PURCHASE_OFFER_PATTERN,
                    uri.path ?: "",
                )
            ) {
                val product = if (uri.pathSegments.size >= 3) uri.pathSegments[2] else null
                val offer = if (uri.pathSegments.size == 4) uri.pathSegments[3] else null
                if (product != null) {
                    return Pair(product, offer)
                }
            }
        }
        return null
    }
}
