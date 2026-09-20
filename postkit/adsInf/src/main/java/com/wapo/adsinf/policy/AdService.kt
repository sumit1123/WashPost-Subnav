package com.wapo.adsinf.policy

import com.wapo.adsinf.models.AdsModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/**
 * AdService provides a clean abstraction for the ad policy.
 * It wraps the repository and is the primary point of contact for the UI layer.
 */
@Singleton
class AdService @Inject constructor(
    private val repository: AdsPolicyRepository
) {
    /**
     * Observable flow of the current ad mode.
     */
    val adsMode: StateFlow<AdsModel> = repository.adsMode

    /**
     * Synchronous check for the current ad mode.
     */
    val currentAdsMode: AdsModel get() = repository.adsMode.value

    /**
     * Observable flow for whether the ad-free subscription is renewable.
     */
    val isAdFreeRenewable: StateFlow<Boolean> = repository.isAdFreeRenewable

    /**
     * Synchronous check for whether the ad-free subscription is renewable.
     */
    val currentIsAdFreeRenewable: Boolean get() = repository.isAdFreeRenewable.value
}
