// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.backendhealth.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.domain.repository.HealthStatusRepo
import com.wapo.flagship.features.backendhealth.models.FailoverArticle
import com.wapo.flagship.features.backendhealth.models.FailoverState
import com.wapo.flagship.features.backendhealth.repository.FailoverRepository
import com.wapo.flagship.network.retrofit.network.APIResult
import com.washingtonpost.android.config.domain.models.config.BackendHealthConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BackendHealthCheckerViewModel @Inject constructor(
    @CoroutineScopeCommonsModule.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val healthStatusRepo: HealthStatusRepo,
    private val remoteLogRepo: RemoteLogRepo,
    private val failoverRepository: FailoverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FailoverState()
    )
    val uiState: StateFlow<FailoverState> = _uiState
    val uiStateLiveData: LiveData<FailoverState> = _uiState.asLiveData()

    fun checkBackendHealth(backendHealthConfig: BackendHealthConfig) {
        _uiState.update {
            it.copy(
                healthCheckIsLoading = true
            )
        }
        viewModelScope.launch(ioDispatcher) {
            val result = healthStatusRepo.fetchHealthStatus(
                    backendHealthConfig.backendHealthMonitorURL,
                    backendHealthConfig.fallbackURL,
                    backendHealthConfig.fallbackStaticURL,
                )

            _uiState.update {
                it.copy(
                    healthCheckIsLoading = false,
                    isHealthy = result.isHealthy,
                    fallbackURL = result.fallbackURL,
                    fallbackStaticURL = result.fallbackStaticURL
                )
            }

            if (!result.isHealthy) {
                Logger.e(TAG, "backend not healthy in Top Stories. Take to degraded experience")
                if (!result.fallbackURL.isNullOrEmpty() || !result.fallbackStaticURL.isNullOrEmpty()) {
                    showFallbackPopUp()
                    // Log only once for a given user
                    if (!healthStatusRepo.isFailoverActive()) {
                        healthStatusRepo.setFailoverActive(true)
                        logStatus()
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = true,
                        )
                    }
                    val failoverPageResponse = failoverRepository.fetchArticles(result.fallbackStaticURL.orEmpty())
                    when (failoverPageResponse) {
                        is APIResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    articles = failoverPageResponse.data?.articles?.map { article ->
                                        with(article) {
                                            FailoverArticle(
                                                headline.orEmpty(),
                                                byline.orEmpty(),
                                                if (!smallthumburl.isNullOrEmpty() && smallthumburl.lowercase() != "none") smallthumburl else null,
                                                contenturl.orEmpty(),
                                                displaydatetime?.toLong(),
                                            )
                                        }
                                    }.orEmpty(),
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    articles = emptyList(),
                                )
                            }
                        }
                    }
                }
            } else {
                if (healthStatusRepo.isFailoverActive()) {
                    healthStatusRepo.setFailoverActive(false)
                    // Log inactive only for users who have been tracked earlier
                    logStatus()
                }
                hideFallbackPopUp()
            }
        }
    }

    private fun showFallbackPopUp() {
        _uiState.update {
            it.copy(
                showFallbackPopUp = true
            )
        }
    }

    private fun hideFallbackPopUp() {
        _uiState.update {
            it.copy(
                showFallbackPopUp = false
            )
        }
    }

    private suspend fun logStatus() {
        EventLog
            .Builder()
            .apply {
                setMessage("Failover status")
                setModule(LogModules.BACKEND_HEALTH)
                set("active", healthStatusRepo.isFailoverActive())
                set(
                    "switch_over",
                    SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).format(
                        Date(System.currentTimeMillis()),
                    ),
                )
            }.run {
                remoteLogRepo.e(this.build())
            }
    }

    companion object {
        private const val TAG = "BackendHealth"
    }
}
