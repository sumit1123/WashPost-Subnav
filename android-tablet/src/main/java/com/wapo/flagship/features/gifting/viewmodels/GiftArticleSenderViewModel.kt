package com.wapo.flagship.features.gifting.viewmodels

import androidx.lifecycle.*
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.gifting.events.UserEvent
import com.wapo.flagship.features.gifting.models.RemainingCountApiStatus
import com.wapo.flagship.features.gifting.models.RequestUrlApiStatus
import com.wapo.flagship.features.gifting.repo.GiftArticleSenderRepo
import com.wapo.flagship.features.gifting.states.GiftSendUiState
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.paywall.PaywallService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This vm is specific to gift sender flow.
 */
@HiltViewModel
class GiftArticleSenderViewModel
    @Inject
    constructor(
        private val giftArticleSenderRepo: GiftArticleSenderRepo,
        private val dispatcherProvider: DispatcherProvider,
    ) : ViewModel() {
        /**
         * Ui state that directly dictates the content of the Gift article sender bottom sheet.
         */
        private val _giftUiState = MediatorLiveData<GiftSendUiState>()
        val giftUiState: LiveData<GiftSendUiState> = _giftUiState

        /**
         * Handle user click events
         */
        private val _userEvent = LiveEvent<UserEvent>()
        val userEvent: LiveData<UserEvent> = _userEvent

        init {
            addRemainingCountRemoteSource()
            addRequestGiftUrlRemoteSource()
            _giftUiState.postValue(GiftSendUiState.Startup)
        }

        /**
         * Start Gifting flow.
         * - User Not Subscribed -> Send NoSub State
         * - User Not Logged In -> Send NotSignedIn State
         * - Else -> Proceed Gift Flow Startup
         */
        fun startGiftingFlow() {
            val uiState =
                when {
                    !PaywallService.getInstance().isPremiumUser -> GiftSendUiState.NoSub
                    !PaywallService.getInstance().isWpUserLoggedIn -> GiftSendUiState.NotSignedIn
                    else -> GiftSendUiState.SignedInSub
                }
            _giftUiState.postValue(uiState)
        }

        /**
         * Call this function only for signed in and subscribed users. This fetches the remaining gift article count for user for the current month.
         */
        fun signedInSubscriberTappedGiftIcon(articleUrl: String) {
            _giftUiState.postValue(GiftSendUiState.Loading)
            viewModelScope.launch(dispatcherProvider.io) {
                giftArticleSenderRepo.getRemainingGiftCount(getUrlWithoutParameters(articleUrl))
            }
        }

        /**
         * Call this function only for signed in and subscribed users. This fetches the url of the article to be gifted.
         */
        fun shareButtonClickedOnGiftBottomSheet(articleUrl: String) {
            _giftUiState.postValue(GiftSendUiState.Loading)
            viewModelScope.launch(dispatcherProvider.io) {
                giftArticleSenderRepo.getGiftArticleTokenWithUrl(getUrlWithoutParameters(articleUrl))
            }
        }

        fun dispatchUserEvent(userEvent: UserEvent) {
            _userEvent.postValue(userEvent)
        }

        /**
         * Adds remote source from [giftArticleSenderRepo]
         */
        private fun addRemainingCountRemoteSource() {
            _giftUiState.addSource(giftArticleSenderRepo.remainingCountStatus) { remainingCountStatus ->
                when (remainingCountStatus) {
                    RemainingCountApiStatus.Failure -> _giftUiState.postValue(GiftSendUiState.Failure)
                    RemainingCountApiStatus.NoRemainingArticles ->
                        _giftUiState.postValue(
                            GiftSendUiState.NoGifts,
                        )
                    is RemainingCountApiStatus.Success -> {
                        val state =
                            if (remainingCountStatus.hasAlreadyShared) {
                                GiftSendUiState.GiftAgain(
                                    remainingCountStatus.remainingCount,
                                )
                            } else {
                                GiftSendUiState.Gift(remainingCountStatus.remainingCount)
                            }
                        _giftUiState.postValue(state)
                    }
                }
            }
        }

        /**
         * Adds remote source from [giftArticleSenderRepo]
         */
        private fun addRequestGiftUrlRemoteSource() {
            _giftUiState.addSource(giftArticleSenderRepo.requestUrlApiStatus) { requestUrlApiStatus ->
                when (requestUrlApiStatus) {
                    RequestUrlApiStatus.Failure -> _giftUiState.postValue(GiftSendUiState.Failure)
                    is RequestUrlApiStatus.Success ->
                        _giftUiState.postValue(
                            GiftSendUiState.GiftTokenUrl(requestUrlApiStatus.url),
                        )
                }
            }
        }
    }
