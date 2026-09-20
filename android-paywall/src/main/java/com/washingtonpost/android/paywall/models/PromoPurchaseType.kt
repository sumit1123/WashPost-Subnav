package com.washingtonpost.android.paywall.models

/**
 * Different type of subscription purchases made by the user.
 */
enum class PromoPurchaseType(val type: String) {
    /**
     * Not a promo purchase
     */
    NONE("none"),

    /**
     * Purchase made out of app but is not defined yet (potentially can be out of app promo purchase).
     */
    UNDEFINED_OUT_OF_APP("undefined_out_of_app"),

    /**
     * Purchase made in app but is not defined yet (potentially can be in app promo purchase).
     */
    UNDEFINED_IN_APP("undefined_in_app"),

    /**
     * Purchase made using a promo code out of app.
     */
    PROMO_OUT_OF_APP("promo_out_of_app"),

    /**
     * Purchase made using a promo code in app.
     */
    PROMO_IN_APP("promo_in_app")
}