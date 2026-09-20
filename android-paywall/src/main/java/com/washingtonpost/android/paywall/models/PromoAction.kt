package com.washingtonpost.android.paywall.models

sealed class PromoAction {

    /**
     * Show a named paywall blocker
     * @param name  Blocker to display
     * @param action Blocker's CTA label
     * @param actionRequirements guardrails for action
     */
    data class Blocker(
        val name: String?,
        val action: String? = null,
        val actionRequirements: List<MessageRequirements>? = null,
    ) : PromoAction()

    /**
     * Show billing for specified product.
     * @param product  Product name (e.g. basic-monthly)
     * @param code    The promotional offer code.
     * @param action   label for CTA
     * @param actionRequirements guardrails for action
     */
    data class Code(
        val product: String?,
        val code: String?,
        val action: String?,
        val actionRequirements: List<MessageRequirements>? = null,
    ) : PromoAction()

    /**
     * Open a URL
     * @param url   The URL to open.
     * @param action label for CTA
     */
    data class Open(
        val url: String?,
        val action: String?,
    ) : PromoAction()
}