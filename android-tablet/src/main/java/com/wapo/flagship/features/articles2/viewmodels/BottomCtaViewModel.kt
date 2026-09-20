package com.wapo.flagship.features.articles2.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.config.domain.models.config.paywall.BottomCtaModel
import com.washingtonpost.android.paywall.BaseSubViewModel
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.bottomsheet.SubState
import com.washingtonpost.android.paywall.bottomsheet.model.BottomCtaToPaywallAction
import com.washingtonpost.android.paywall.bottomsheet.model.BottomCtaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BottomCtaViewModel
    @Inject
    constructor(
        val dispatcherProvider: DispatcherProvider,
    ) : BaseSubViewModel() {
        private val bottomCtaModel
            get() = PaywallService.getInstance()?.bottomCtaModel ?: BottomCtaModel.backupModel

        val sku
            get() = bottomCtaModel.productId

        private val _ctaType: MediatorLiveData<BottomCtaType> = MediatorLiveData()
        val ctaType: LiveData<BottomCtaType> = _ctaType

        private val _bottomCtaToPaywallAction: LiveEvent<BottomCtaToPaywallAction> = LiveEvent()
        val bottomCtaToPaywallAction: LiveData<BottomCtaToPaywallAction> = _bottomCtaToPaywallAction

        /**
         * Send appropriate CTA Type depending on:
         * - Valid Gift Article -> [BottomCtaType.GIFT]
         * - All other cases -> [BottomCtaType.NONE]
         */
        fun dispatchCtaType(type: BottomCtaType) {
            _ctaType.postValue(type)
        }

        /**
         * Subscribe text for Gift CTA
         */
        fun getSubText(): LiveData<String> =
            subStateLiveData.map { subState ->
                when (subState) {
                    SubState.NoSub -> "Subscribe"
                    SubState.TerminatedSub -> "Resubscribe"
                    SubState.FreeTrialSub -> {
                        if (isIapTerminated) {
                            "Resubscribe"
                        } else {
                            "Subscribe"
                        }
                    }
                    SubState.ActiveSub, SubState.PausedSub -> {
                        // FIXME: Update text for Amazon Dollar Offer. Need to get appropriate text for this case
                        "Already Subscribed"
                    }
                }
            }

        /**
         * When user clicks on the bottom cta send cta to paywall action to the lifecycle owner.
         */
        fun bottomCtaClicked() {
            viewModelScope.launch(dispatcherProvider.main) {
                _bottomCtaToPaywallAction.value =
                    if (_ctaType.value == BottomCtaType.GIFT) {
                        BottomCtaToPaywallAction.GiftBottomCtaToPaywallAction()
                    } else {
                        BottomCtaToPaywallAction.DefaultBottomCtaToPaywallAction()
                    }
            }
        }
    }
