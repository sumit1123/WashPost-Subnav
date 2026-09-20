package com.wapo.adsinf.policy

import com.washingtonpost.android.paywall.PaywallReactive
import com.wapo.adsinf.models.AdsModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Singleton
class AdsPolicyRepository @Inject constructor(
    private val subAttributeAdFreeKey: String,
    private val subAttributeAdFreeEuKey: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _adsMode = MutableStateFlow<AdsModel>(AdsModel.Enabled)
    val adsMode: StateFlow<AdsModel> = _adsMode

    private val _isAdFreeRenewable = MutableStateFlow(true)
    val isAdFreeRenewable: StateFlow<Boolean> = _isAdFreeRenewable

    init {
        scope.launch {
            PaywallReactive.isAdFreeRenewable.collect {
                _isAdFreeRenewable.value = it
            }
        }

        scope.launch {
            combine(
                PaywallReactive.isAdFree,
                PaywallReactive.subAttributes
            ) { isAdFree, subAttributes ->
                val hasSubAttributeAdFree = PaywallReactive.isAdFreeInAttributes(
                    subAttributes,
                    subAttributeAdFreeKey,
                    subAttributeAdFreeEuKey
                )

                val isAdFree = isAdFree || hasSubAttributeAdFree
                isAdFree
            }
            .distinctUntilChanged()
            .collect { isAdFree ->
                val newMode = if (isAdFree) AdsModel.Disabled else AdsModel.Enabled
                _adsMode.value = newMode
            }
        }
    }
}
