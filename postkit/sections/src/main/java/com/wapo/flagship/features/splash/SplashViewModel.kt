package com.wapo.flagship.features.splash

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.domain.UtilsRepo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogKeys
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.agerestriction.AgeRestrictionValidator
import com.wapo.android.commons.util.agerestriction.AgeRestrictionsError
import com.wapo.android.commons.util.agerestriction.AgeState
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeSetUp
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val remoteLogRepo: RemoteLogRepo,
    private val utilsRepo: UtilsRepo
) : ViewModel() {

    private val _splashReadyToBeDismissed = MutableLiveData(SplashModel())
    val splashReadyToBeDismissed: LiveData<SplashModel> = _splashReadyToBeDismissed

    fun dismissSplashScreen() {
        _splashReadyToBeDismissed.value = _splashReadyToBeDismissed.value?.copy(
            dismissSplash = true
        )
    }

    fun isAgeEligible(
        activity: Activity,
        ageRestrictionValidator: AgeRestrictionValidator,
        ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check age restriction only if enabled in config
            val ageRestrictionConfig = ConfigManager.getInstance().config.ageRestriction
            val isAgeRestrictionEnabled = if (utilsRepo.isProductFlavorAmazon()) {
                val minAmazonOsVersion = ageRestrictionConfig.minAmazonOsVersion
                val isSupported = minAmazonOsVersion != null && utilsRepo.getOsVersion() >= minAmazonOsVersion
                isSupported && ageRestrictionConfig.amazonEnabled
            } else {
                if (utilsRepo.isChromebook()) {
                    ageRestrictionConfig.chromebookEnabled
                } else {
                    ageRestrictionConfig.androidEnabled
                }
            }
            Logger.d(TAG, "ageRestrictionConfig => $ageRestrictionConfig")
            Logger.d(TAG, "isProductFlavorAmazon => ${utilsRepo.isProductFlavorAmazon()}")
            Logger.d(TAG, "isAgeRestrictionEnabled => $isAgeRestrictionEnabled")
            if (isAgeRestrictionEnabled) {
                ageRestrictionValidator.isAgeEligible(
                    activity = activity,
                    ageRestrictionsFakeSetUp = ageRestrictionsFakeSetUp,
                ) { result ->
                    val resultState = when (result) {
                        is AgeState.Restricted -> {
                            logEventRestriction("state Age Restricted")
                            SplashModel(navigateToBlockAgeRestriction = true, loadingAgeRestriction = false)
                        }
                        is AgeState.Success -> {
                            goToHomePage()
                        }
                        is AgeState.Unknown -> {
                            logEventRestriction("state Unknown")
                            SplashModel(navigateToUnknownState = true, loadingAgeRestriction = false)
                        }
                        is AgeState.ParentPermission -> {
                            logEventRestriction("state ParentPermission require")
                            SplashModel(navigateToParentPermissionsBlockRestriction = true, loadingAgeRestriction = false)
                        }
                        is AgeState.VisitPlayStore -> {
                            logEventRestriction("state Visit PlayStore require")
                            SplashModel(navigateToVisitPlayStore = true, loadingAgeRestriction = false)
                        }
                        is AgeState.Error -> {
                            logEventRestriction("state Api Error => ${result.error.code}")
                            when(result.error) {
                                AgeRestrictionsError.ApiNotAvailable,
                                AgeRestrictionsError.PlayStoreNotFound,
                                AgeRestrictionsError.NetworkError,
                                AgeRestrictionsError.PlayServicesNotFound,
                                AgeRestrictionsError.CannotBindToService,
                                AgeRestrictionsError.PlayStoreVersionOutdated,
                                AgeRestrictionsError.PlayServicesVersionOutdated,
                                AgeRestrictionsError.AppNotOwned,
                                AgeRestrictionsError.SdkVersionOutdated,
                                AgeRestrictionsError.AmazonAppNotOwned -> {
                                    SplashModel(
                                        showModalAgeRestrictionError = true,
                                        showModalAgeRestrictionErrorTitle = result.error.title,
                                        showModalAgeRestrictionErrorMessage = result.error.error,
                                        loadingAgeRestriction = false,
                                        blockUser = result.error.block
                                    )
                                }
                                AgeRestrictionsError.ClientTransientError, AgeRestrictionsError.InternalError -> {
                                    SplashModel(retryCall = true, loadingAgeRestriction = false)
                                }
                                AgeRestrictionsError.UnknownError,
                                AgeRestrictionsError.AmazonFeaturedNotSupportedError -> {
                                    SplashModel(navigateToUnknownState = true, loadingAgeRestriction = false)
                                }
                            }
                        }
                        is AgeState.RequireUpdate -> {
                            logEventRestriction("state RequireUpdate")
                            SplashModel(showModalAgeRestrictionUpgrade = true, loadingAgeRestriction = false)
                        }
                    }

                    _splashReadyToBeDismissed.postValue(resultState)
                }
            } else {
                _splashReadyToBeDismissed.postValue(goToHomePage())
            }
        }
    }

    private fun logEventRestriction(message: String) {
        val eventLog = EventLog.Builder()
            .setModule(LogModules.AGE_RESTRICTION)
            .setMessage("Age Restriction Result")
            .set(LogKeys.MANUFACTURER.keyName, utilsRepo.getManufacturerValue())
            .setErrorMessage(message)
            .build()
        remoteLogRepo.w(eventLog)
    }

    private fun goToHomePage(): SplashModel? {
        return _splashReadyToBeDismissed.value?.copy(
            retryCall = false,
            loadingAgeRestriction = false
        )
    }

    private companion object {
        const val TAG = "SplashViewModel"
    }
}
