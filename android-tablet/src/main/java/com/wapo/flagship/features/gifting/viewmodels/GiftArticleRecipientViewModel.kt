package com.wapo.flagship.features.gifting.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.events.GiftState
import com.washingtonpost.android.paywall.features.tetro.TetroException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GiftArticleRecipientViewModel
    @Inject
    constructor(
        @ApplicationContext private val applicationContext: Context,
        private val dispatcherProvider: DispatcherProvider,
    ) : ViewModel() {
        private val GIFT_PARAM = "pwapi_token"

        /**
         * States for identifying Gift Tokens and processing them.
         */
        private val _giftArticleState: LiveEvent<GiftState> = LiveEvent()
        val giftArticleState: LiveEvent<GiftState> = _giftArticleState

        init {
            _giftArticleState.postValue(GiftState.None)
        }

        /**
         * Set state of Gift Token
         */
        fun dispatchGiftState(state: GiftState) {
            _giftArticleState.postValue(state)
        }

        /**
         * Check if Gift has been handled.
         */
        private fun isGiftHandled(): Boolean =
            when (_giftArticleState.value) {
                GiftState.Expired, GiftState.NotGift, GiftState.ValidNotExpired -> true
                else -> false
            }

        /**
         * Process Gift Token
         */
        fun processGiftToken(
            token: String?,
            url: String?,
        ) {
            if (!token.isNullOrEmpty() && !url.isNullOrEmpty() && !isGiftHandled()) {
                viewModelScope.launch(dispatcherProvider.io) {
                    val resultState =
                        PaywallService.getInstance().tetroManager.processGiftToken(
                            token,
                            url,
                            Measurement.getABTestingVariants(applicationContext),
                        )
                    if (resultState is GiftState.Failure) {
                        PaywallService
                            .getConnector()
                            .logHandledException(TetroException(resultState.message ?: "Gift Error"))
                    }
                    dispatchGiftState(resultState)
                }
            }
        }
    }
