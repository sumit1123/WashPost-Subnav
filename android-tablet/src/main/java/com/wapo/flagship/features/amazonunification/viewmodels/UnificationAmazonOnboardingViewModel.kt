package com.wapo.flagship.features.amazonunification.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.amazonunification.models.AccountSubState
import com.wapo.flagship.features.amazonunification.models.UserClickEvent
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.paywall.PaywallService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * This view model facilitates different states in the unification onboarding screen(s) (probably only one)
 */
@HiltViewModel
class UnificationAmazonOnboardingViewModel
    @Inject
    constructor(
        private val dispatcherProvider: DispatcherProvider,
    ) : ViewModel() {
        /**
         * This livedata propagates the user click events from a fragment to it's activity.
         */
        private val _userClickEvent = LiveEvent<UserClickEvent>()
        val userClickEvent: LiveData<UserClickEvent> = _userClickEvent

        /**
         * This live data propagates the account and subscription info to lifecycle owner that needs it.
         */
        private val _accountSubState = MutableLiveData<AccountSubState>()
        val accountSubState: LiveData<AccountSubState> = _accountSubState

        /**
         * This live data propagates info to activity in case there are any errors e.g. when wp user is logged in but we could not get
         * user id/email or their display name.
         */
        private val _remoteLogger = MutableLiveData<String>()
        val remoteLogger: LiveData<String> = _remoteLogger

        /**
         * When user taps on the CTA on the screen, this function propagates that info to the activity.
         */
        fun getStartedClicked() {
            _userClickEvent.value = UserClickEvent.GetStartedClicked
        }

        /**
         * When user taps on the Sign-in link text on the screen, this function propagates that info to the activity.
         */
        fun signInClicked() {
            _userClickEvent.value = UserClickEvent.SignInClicked
        }

        /**
         * When user taps on the Learn More link text on the screen, this function propagates that info to the activity.
         */
        fun learnMoreClicked() {
            _userClickEvent.value = UserClickEvent.LearnMoreClicked
        }

        /**
         * Dispatches the value to the observer in this case UnificationAmazonOnboardingFragment.
         */
        fun setAccountAndSubState(accountSubState: AccountSubState) {
            _accountSubState.value = accountSubState
        }

        /**
         * Load profile again if user has migrated account
         */
        fun initAccountProfile() {
            if (PaywallService.getInstance()?.isWpUserLoggedIn == true) {
                PaywallService.getInstance()?.fetchUserProfile()
            }
        }

        /**
         * This function initializes the account and subs state so that it is set appropriately.
         */
        fun initializeSettingSubAndAccountState() {
            if (PaywallService.getInstance() == null) {
                _remoteLogger.value =
                    "Unification AmazonOnboarding Screen : Aborted showing unification onboarding screen because PaywallService has not been initialized"
                setAccountAndSubState(AccountSubState.GenericState)
                return
            }
            val loggedInUser = PaywallService.getInstance().loggedInUser
            when {
                loggedInUser == null -> {
                    /**
                     * Set [AccountSubState.NoAccountWithSub] if user has migrated rainbow subscription.
                     * Otherwise show them the generic screen that we show for No Account / No sub scenario.
                     */
                    PaywallService.getBillingHelper()?.migratedRainbowSubscription?.let {
                        setAccountAndSubState(AccountSubState.NoAccountWithSub)
                    } ?: setAccountAndSubState(AccountSubState.GenericState)
                }
                /**
                 * Try to set userId which is email first. If that's null (e.g. login via facebook etc), pick the display name.
                 */
                loggedInUser.userId != null ->
                    setAccountAndSubState(
                        AccountSubState.AccountPresent(loggedInUser.userId),
                    )
                loggedInUser.displayName != null ->
                    setAccountAndSubState(
                        AccountSubState.AccountPresent(loggedInUser.displayName),
                    )
                else -> {
                    _remoteLogger.value =
                        "Unification AmazonOnboarding Screen : Logged in user does not have email/display name to show on unification onboarding screen"
                    setAccountAndSubState(AccountSubState.GenericState)
                }
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
    }
