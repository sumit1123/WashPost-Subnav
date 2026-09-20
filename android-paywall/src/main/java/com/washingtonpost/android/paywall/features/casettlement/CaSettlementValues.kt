package com.washingtonpost.android.paywall.features.casettlement

/**
 * These are the different values that will be shown on the CA settlement dialog and change depending on the subs type, price and expiration date.
 */
data class CaSettlementValues(val unFormattedExpirationDate: String, val price: String, val productType: String)
