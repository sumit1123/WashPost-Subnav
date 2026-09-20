package com.washingtonpost.android.paywall

import com.washingtonpost.android.paywall.newdata.response.SubItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Small reactive holder for paywall-related reactive state.
 * Updated by the Paywall connector (server/profile), billing helpers, and observed by the AdsPolicyRepository.
 */
object PaywallReactive {

    const val AD_FREE_PRODUCT = "AD_FREE"

    const val PREMIUM_PRODUCT = "premium"

    const val BASIC_PRODUCT = "basic"

    // Flow for server-side subscription attributes
    private val _subAttributes = MutableStateFlow<Set<String>?>(null)
    val subAttributes: StateFlow<Set<String>?> = _subAttributes

    // Combined ad-free status (OR of all sources)
    private val _isAdFree = MutableStateFlow(false)
    val isAdFree: StateFlow<Boolean> = _isAdFree

    // Flow for ad-free renewal status
    private val _isAdFreeRenewable = MutableStateFlow(false)
    val isAdFreeRenewable: StateFlow<Boolean> = _isAdFreeRenewable

    // Individual source tracking for ad-free
    private var _isAdFreeFromBilling = false
    private var _isAdFreeFromVerify = false
    private var _isAdFreeFromProfile = false

    /**
     * Observed by [IterableActivityViewModel]
     * Triggered when re-evaluation of placement visibility is necessary (e.g. subscription changes)
     */
    private val _updateIterablePlacement = MutableStateFlow(false)
    val updateIterablePlacement: StateFlow<Boolean> = _updateIterablePlacement

    @JvmStatic
    fun notifyPlacementChanged() {
        _updateIterablePlacement.value = !_updateIterablePlacement.value
    }

    /**
     * Check if a SubItem represents an ad-free product.
     */
    @JvmStatic
    fun isAdFreeProduct(subItem: SubItem?): Boolean {
        return AD_FREE_PRODUCT.equals(subItem?.product, ignoreCase = true)
    }

    // Tracks the last /verify device call result.
    // null = no verify attempted yet, true = success, false = failure
    private val _verifyComplete = MutableStateFlow<Boolean?>(null)
    val verifyComplete: StateFlow<Boolean?> = _verifyComplete


    @JvmStatic
    fun notifyVerifyComplete(success: Boolean) {
        _verifyComplete.value = success
    }

    @JvmStatic
    fun resetVerifyState() {
        _verifyComplete.value = null
    }

    @JvmStatic
    fun updateSubAttributes(attrs: Set<String>?) {
        _subAttributes.value = attrs
    }

    /**
     * Converts server subscription attributes to key:value format used by the
     * persisted attribute set. Keeping the value is required for attributes such as
     * NOADS:1, where the key alone does not grant the entitlement.
     */
    @JvmStatic
    fun updateSubAttributes(attrs: Map<String, String>?) {
        _subAttributes.value = attrs?.mapTo(LinkedHashSet()) { (key, value) -> "$key:$value" }
    }

    /**
     * Update ad-free status from the device's billing/store purchases.
     * The combined ad-free status is the OR of all sources.
     */
    @JvmStatic
    fun updateAdFreeFromBilling(isAdFree: Boolean) {
        _isAdFreeFromBilling = isAdFree
        refreshAdFreeStatus()
    }

    /**
     * Update ad-free status from the /verify device subscription response.
     * The combined ad-free status is the OR of all sources.
     */
    @JvmStatic
    fun updateAdFreeFromVerify(isAdFree: Boolean) {
        _isAdFreeFromVerify = isAdFree
        refreshAdFreeStatus()
    }

    /**
     * Update ad-free status from the /profile (logged-in user) response.
     * The combined ad-free status is the OR of all sources.
     */
    @JvmStatic
    fun updateAdFreeFromProfile(isAdFree: Boolean) {
        _isAdFreeFromProfile = isAdFree
        refreshAdFreeStatus()
    }

    /**
     * Recomputes the combined ad-free status from all sources.
     */
    private fun refreshAdFreeStatus() {
        val combined = _isAdFreeFromBilling || _isAdFreeFromVerify || _isAdFreeFromProfile
        if (_isAdFree.value != combined) {
            _isAdFree.value = combined
        }
    }

    @JvmStatic
    fun updateAdFreeRenewableStatus(isRenewable: Boolean) {
        if (_isAdFreeRenewable.value != isRenewable) {
            _isAdFreeRenewable.value = isRenewable
        }
    }

    @JvmStatic
    fun isAdFreeInAttributes(attrs: Set<String>?, adFreeKey: String, adFreeEuKey: String): Boolean {
        if (attrs == null) return false
        return attrs.contains(adFreeKey) || attrs.contains(adFreeEuKey)
    }

    @JvmStatic
    fun reset() {
        _subAttributes.value = null
        _isAdFreeFromProfile = false
        _updateIterablePlacement.value = false
        refreshAdFreeStatus()
        resetVerifyState()
    }
}
