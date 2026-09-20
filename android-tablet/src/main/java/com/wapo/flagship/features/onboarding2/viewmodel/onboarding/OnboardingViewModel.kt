/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.onboarding2.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.domain.repository.OnboardingRepo
import com.wapo.flagship.features.onboarding2.models.AccountSubState
import com.wapo.flagship.features.onboarding2.models.OnboardingEvent
import com.wapo.flagship.features.onboarding2.models.OnboardingUiState
import com.wapo.flagship.features.onboarding2.models.UserClickEvent
import com.wapo.flagship.model.LwaProfile
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthEntryPoint
import com.washingtonpost.android.paywall.util.PaywallConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * This view model facilitates different states in the onboarding screen(s) (probably only one)
 */
@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val onboardingRepo: OnboardingRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState()
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _onboardingEvent: MutableSharedFlow<OnboardingEvent> = MutableSharedFlow(replay = 0)
    val onboardingEvent: SharedFlow<OnboardingEvent> = _onboardingEvent

    /**
     * Determines when App Store has been queried and IAP Status has been discovered. Will be used to determine when
     * Onboarding screen will be shown.
     */
    private val _iapStatusKnown = LiveEvent<Boolean>()
    val iapStatusKnown: LiveEvent<Boolean> = _iapStatusKnown

    /**
     * Is user signed in.
     */
    val isSignedIn
        get() = PaywallService.getInstance() != null && PaywallService.getInstance().isWpUserLoggedIn

    private var iapSubStatusInitialized = false

    fun initIapSubStatus() {
        if (PaywallService.getInstance() != null && !iapSubStatusInitialized) {
            PaywallService.getConnector()?.iapSubStatus?.let {
                _iapStatusKnown.addSource(it) { subStatus ->
                    val isKnown =
                        when (subStatus) {
                            PaywallConstants.IapSubStatus.UNKNOWN -> false
                            else -> true
                        }
                    _iapStatusKnown.postValue(isKnown)
                }
                iapSubStatusInitialized = true
            }
        }
    }

    /**
     * When user interacts with app push prompt request.
     */
    fun pushPromptResponse(response: Boolean) {
        viewModelScope.launch {
            _onboardingEvent.emit(
                OnboardingEvent.AcceptsPushNotifications(response)
            )
        }
    }

    /**
     * When user taps on the Sign-in link text on the screen, this function propagates that info to the activity.
     */
    fun signInClicked() {
        viewModelScope.launch {
            _onboardingEvent.emit(
                UserClickEvent.SignIn
            )
        }
    }

    /**
     * When user taps on the Create Account button on the screen, this function propagates that info to the activity.
     */
    fun createAccountClicked() {
        viewModelScope.launch {
            _onboardingEvent.emit(
                UserClickEvent.CreateAccount
            )
        }
    }

    /**
     * When user taps on the Learn More link text on the screen, this function propagates that info to the activity.
     */
    fun subscribeClicked() {
        viewModelScope.launch {
            _onboardingEvent.emit(
                UserClickEvent.Subscribe
            )
        }
    }

    /**
     * Dismiss the activity that is running the flow
     */
    fun dismissClicked() {
        viewModelScope.launch {
            _onboardingEvent.emit(
                UserClickEvent.Dismiss
            )
        }
    }

    /**
     * Dismiss the activity that is running the flow
     */
    fun continueClicked() {
        val step = onboardingRepo.getCurrentStep()
        val stepSize = onboardingRepo.getStepSize()
        if (step < stepSize) {
            viewModelScope.launch {
                _onboardingEvent.emit(
                    UserClickEvent.Continue
                )
            }
        } else {
            viewModelScope.launch {
                _onboardingEvent.emit(
                    UserClickEvent.Dismiss
                )
            }
        }
    }

    /**
     * Dismiss the activity that is running the flow
     */
    fun backClicked() {
        viewModelScope.launch {
            _onboardingEvent.emit(
                UserClickEvent.Back
            )
        }
    }

    /**
     * Dispatches the value to the observer in this case UnificationAmazonOnboardingFragment.
     */
    fun setAccountAndSubState(accountSubState: AccountSubState) {
        _uiState.update {
            it.copy(
                accountSubState = accountSubState
            )
        }
    }

    /**
     * This function initializes the account and subs state so that it is set appropriately.
     */
    fun initializeSettingSubAndAccountState() {
        if (PaywallService.getInstance() == null) {
            setAccountAndSubState(AccountSubState.NoAccountNoSub(false))
            return
        }
        val isLoggedIn = PaywallService.getInstance().isWpUserLoggedIn
        val isPremiumUser = PaywallService.getInstance().isPremiumUser
        val isTerminated = PaywallService.getInstance().isSubscriptionTerminated
        val isLwaLoggedIn =
            PrefUtils.getDeviceProfile(
                FlagshipApplication.Companion.getInstance(),
                LwaProfile::class.java,
            ) != null
        when {
            /*
             Order -
                1) Check if user is Logged in (or LWA logged in) AND has a sub - dismiss the dialog in this case
                2) If they are LWA logged in hide sign in button (for LWA migration to take place in the background) and
                   show Subscribe button only.
                3) If they are a premium user that means they are ot logged in (or LWA logged in) - Show them sign in
                4) They are logged in using WPAA (not LWA) and their sub is terminated - Show them Subscribe
             */
            isPremiumUser && (isLoggedIn || isLwaLoggedIn) ->
                setAccountAndSubState(
                    AccountSubState.AccountWithSub,
                )

            isLwaLoggedIn -> setAccountAndSubState(AccountSubState.LwaLoggedIn(isTerminated))
            isPremiumUser -> setAccountAndSubState(AccountSubState.NoAccountWithSub)
            isLoggedIn -> setAccountAndSubState(AccountSubState.AccountNoSub(isTerminated))
            else -> setAccountAndSubState(AccountSubState.NoAccountNoSub(isTerminated))
        }
    }

    /**
     * Log user our when user clicks on sign in as someone else.
     * [onComplete] will start the login flow again after user has successfully logged out.
     */
    fun logOutUser(onComplete: () -> Unit = {}) {
        viewModelScope.launch(dispatcherProvider.io) {
            PaywallService.getInstance().logOutCurrentUser()
            withContext(dispatcherProvider.main) {
                onComplete()
            }
        }
    }

    /**
     * Show if user is not logged in and not a premium user.
     */
    fun shouldShow(): Boolean =
        (PaywallService.getInstance()?.isWpUserLoggedIn != true || PaywallService.getInstance()?.isPremiumUser != true) &&
                !isAlreadyShown()

    /**
     * Check if onboarding screen has already been shown
     */
    fun isAlreadyShown(): Boolean {
        return onboardingRepo.isAlreadyShown()
    }

    /**
     * Set Onboarding screen as shown
     */
    fun setShown() {
        onboardingRepo.setShown()
    }

    fun loadPersonalizePages(authEntryPoint: AuthEntryPoint? = null) {
        onboardingRepo.loadPersonalizePages(authEntryPoint)
    }

    fun getCurrentPageId(): Int? {
        return onboardingRepo.getCurrentPageId()
    }

    /**
     * Analytics tracking for current screen.
     */
    fun trackCurrentScreen() {
        val currentPageId = onboardingRepo.getCurrentPageId()

        // Screen has been seen once already so no need to track again
        if (onboardingRepo.isCurrentScreenAlreadySeen()) {
            return
        }

        // Map screen to specifc analytics miscellany value
        val screen =
            when (currentPageId) {
                R.id.action_global_alertsFragment -> Measurement.PROFILE_PREFERENCE_ALERTS
                R.id.action_global_contentPacksFragment -> Measurement.PROFILE_PREFERENCE_CONTENT_PACK
                R.id.action_global_audioFragment -> Measurement.PROFILE_PREFERENCE_AUDIO
                else -> NavigationBehavior.ONBOARDING.value
            }

        // Track onboarding_seen event with screen name
        Measurement.trackOnboardingSeen(screen)

        // Add page to seen list so we track analytics only once.
        currentPageId?.let {
            onboardingRepo.addSeenPage(currentPageId)
        }
    }

    fun nextPage() {
        onboardingRepo.nextPage()
    }

    fun previousPage() {
        onboardingRepo.previousPage()
    }

    fun isFirstPage() = onboardingRepo.isFirstPage()

    fun shouldShowSteps() = onboardingRepo.shouldShowSteps()

    fun shouldShowBack() = onboardingRepo.shouldShowBack()

    fun setAlertsContinueActive(value: Boolean) {
        onboardingRepo.setAlertsContinueActive(value)
    }

    fun setContentPacksContinueActive(value: Boolean) {
        onboardingRepo.setContentPacksContinueActive(value)
    }

    fun getAlertsContinueActive(): Boolean = onboardingRepo.getAlertsContinueActive()

    fun getContentPacksContinueActive(): Boolean = onboardingRepo.getContentPacksContinueActive()

    fun getStepSize() = onboardingRepo.getStepSize()

    fun getCurrentStep() = onboardingRepo.getCurrentStep()
}
