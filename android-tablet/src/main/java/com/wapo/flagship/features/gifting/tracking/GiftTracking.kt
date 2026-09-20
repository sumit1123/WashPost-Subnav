package com.wapo.flagship.features.gifting.tracking

val GIFT_SIGN_IN = "gift_article_share"

enum class GiftTrackingDetails(
    val trackingName: String,
) {
    GIFT_CLICK("gift_article_button_click"),
    SHARE_BUTTON_CLICK("gift_article_share"),
    GIFT_CLICK_NONE_LEFT("gift_article_limit_error"),
    GIFT_FAILURE("gift_article_link_error"),
}
