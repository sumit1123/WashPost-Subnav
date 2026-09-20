package com.wapo.flagship.features.preferencesapi.repo

import android.content.Context
import android.os.Build
import com.wapo.android.commons.constants.AUTHORIZATION
import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.CLIENT_APP_VERSION
import com.wapo.android.commons.constants.CLIENT_ID
import com.wapo.android.commons.constants.CLIENT_IP
import com.wapo.android.commons.constants.CLIENT_USER_AGENT
import com.wapo.android.commons.constants.DEVICE_ID
import com.wapo.android.commons.constants.DEVICE_NAME
import com.wapo.android.commons.constants.OS_VERSION
import com.wapo.android.commons.constants.REQUEST_ID
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.preferencesapi.GetUserNewsprintAttributesApiStatus
import com.wapo.flagship.features.preferencesapi.GetUserNewsprintStateApiStatus
import com.wapo.flagship.features.preferencesapi.models.NewsprintAttributesPreferenceValueItem
import com.wapo.flagship.features.preferencesapi.models.NewsprintAttributesResponse
import com.wapo.flagship.features.preferencesapi.models.NewsprintAttributesValueItem
import com.wapo.flagship.features.preferencesapi.models.NewsprintStatePreferenceValueItem
import com.wapo.flagship.features.preferencesapi.models.NewsprintStateResponse
import com.wapo.flagship.features.preferencesapi.models.NewsprintStateValueItem
import com.wapo.flagship.features.preferencesapi.repo.ContentPacksRepo.Companion.SUCCESS
import com.wapo.flagship.features.preferencesapi.services.PreferencesApiService
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.setNewsprintEngagedStatus
import com.wapo.flagship.util.setNewsprintHasViewed
import com.wapo.flagship.util.setNewsprintReaderType
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * A repository for accessing newsprint service
 * Functions of this repo
 */
class NewsprintRepo
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        private val preferencesApiService: PreferencesApiService,
        private val dispatcherProvider: DispatcherProvider,
    ) {
        /**
         * Mediator live-data to post [GetUserNewsprintAttributesApiStatus] to viewmodel
         */
        val getroNewsprintAttributesStatus = LiveEvent<GetUserNewsprintAttributesApiStatus>()

        /**
         * Mediator live-data to post [GetUserNewsprintStateApiStatus] to viewmodel
         */
        val getroNewsprintStateStatus = LiveEvent<GetUserNewsprintStateApiStatus>()

        /**
         * Required headers for the getro / setro calls.
         */
        private fun getHeaders(): HashMap<String, String> {
            val headers = getBaseHeaders()
            headers[AUTHORIZATION] =
                "Bearer " + AuthHelper.getInstance(FlagshipApplication.getInstance().applicationContext).accessToken
            headers[CLIENT_ID] = PaywallService.getConnector().clientId
            headers[CLIENT_IP] = PaywallService.getConnector().ipAddress
            return headers
        }

        private fun getBaseHeaders(): HashMap<String, String> =
            hashMapOf(
                Pair(CLIENT_APP, PaywallService.getConnector().appName),
                Pair(REQUEST_ID, UUID.randomUUID().toString()),
                Pair(DEVICE_ID, PaywallService.getConnector().deviceId),
                Pair(CLIENT_USER_AGENT, PaywallService.getConnector().userAgent),
                Pair(CLIENT_APP_VERSION, PaywallService.getConnector().appVersion),
                Pair(OS_VERSION, Build.VERSION.SDK_INT.toString()),
                Pair(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL),
            )

        suspend fun getUserNewsprintAttributes() =
            withContext(dispatcherProvider.io) {
                AuthHelper.getInstance(context).runWithValidToken {
                    CoroutineScope(dispatcherProvider.io).launch {
                        try {
                            val result =
                                preferencesApiService.getNewsprintAttributes(
                                    headers = getHeaders(),
                                )

                            processGetNewsprintAttributes(result)
                        } catch (e: Exception) {
                            val builder: EventLog.Builder = EventLog.Builder()
                            builder
                                .setMessage("getUserNewsprintAttributes request failed")
                                .setModule(LogModules.PREFERENCES)
                                .setErrorMessage(e.message)
                                .set("cause", e.cause)
                            RemoteLog.e(context, builder.build())
                        }
                    }
                }
            }

        suspend fun getUserNewsprintState() =
            withContext(dispatcherProvider.io) {
                AuthHelper.getInstance(context).runWithValidToken {
                    CoroutineScope(dispatcherProvider.io).launch {
                        try {
                            val result =
                                preferencesApiService.getNewsprintState(
                                    headers = getHeaders(),
                                )

                            processGetNewsprintState(result)
                        } catch (e: Exception) {
                            val builder: EventLog.Builder = EventLog.Builder()
                            builder
                                .setMessage("getUserNewsprintState request failed")
                                .setModule(LogModules.PREFERENCES)
                                .setErrorMessage(e.message)
                                .set("cause", e.cause)
                            RemoteLog.e(context, builder.build())
                        }
                    }
                }
            }

        private suspend fun processGetNewsprintAttributes(result: APIResult<NewsprintAttributesResponse>) {
            when (result) {
                is APIResult.Success -> {
                    val data: NewsprintAttributesResponse? = result.data

                    if (
                        data?.status?.equals(SUCCESS, ignoreCase = true) == true
                    ) {
                        val preferences = data.preferenceValues.orEmpty()

                        val lastUpdated =
                            preferences
                                .mapNotNull { preferenceValue ->
                                    preferenceValue?.lastUpdated
                                }.maxOrNull() ?: 0L
                        preferences.filterNotNull().forEach { item: NewsprintAttributesPreferenceValueItem ->
                            processNewsprintAttributesPreferenceValueItem(
                                newsprintAttributeValueItem = item,
                                lastUpdated = lastUpdated,
                            )
                        }
                        PreferencesSyncCoordinator.updateFromRemote(
                            context,
                            PreferencesSyncCoordinator.NEWSPRINT_ATTRIBUTES,
                            lastUpdated
                        )
                    } else {
                        getroNewsprintAttributesStatus.postValue(
                            GetUserNewsprintAttributesApiStatus.Failure(
                                "$NS_GET_FAILURE response=$data",
                            ),
                        )
                    }
                }

                is APIResult.Failure, is APIResult.NetworkError -> {
                    getroNewsprintAttributesStatus.postValue(
                        GetUserNewsprintAttributesApiStatus.Failure(
                            "$NS_GET_FAILURE ${result.getMessage()}",
                        ),
                    )
                }
            }
        }

        private suspend fun processGetNewsprintState(result: APIResult<NewsprintStateResponse>) {
            when (result) {
                is APIResult.Success -> {
                    val data: NewsprintStateResponse? = result.data

                    if (
                        data?.status?.equals(SUCCESS, ignoreCase = true) == true
                    ) {
                        val preferences = data.preferenceValues.orEmpty()

                        val lastUpdated =
                            preferences
                                .mapNotNull { preferenceValue ->
                                    preferenceValue?.lastUpdated
                                }.maxOrNull() ?: 0L

                        preferences.filterNotNull().forEach { item: NewsprintStatePreferenceValueItem ->
                            processNewsprintStatePreferenceValueItem(
                                newsprintStateValueItem = item,
                                lastUpdated = lastUpdated,
                            )
                        }
                        PreferencesSyncCoordinator.updateFromRemote(
                            context,
                            PreferencesSyncCoordinator.NEWSPRINT_STATE,
                            lastUpdated
                        )
                    } else {
                        getroNewsprintStateStatus.postValue(
                            GetUserNewsprintStateApiStatus.Failure(
                                "$NS_GET_FAILURE response=$data",
                            ),
                        )
                    }
                }

                is APIResult.Failure, is APIResult.NetworkError -> {
                    getroNewsprintStateStatus.postValue(
                        GetUserNewsprintStateApiStatus.Failure("$NS_GET_FAILURE ${result.getMessage()}"),
                    )
                }
            }
        }

        private suspend fun processNewsprintAttributesPreferenceValueItem(
            newsprintAttributeValueItem: NewsprintAttributesPreferenceValueItem,
            lastUpdated: Long,
        ) {
            newsprintAttributeValueItem.value?.let { value: NewsprintAttributesValueItem ->

                if (value.period?.trim()?.equals(PERIOD, true) == false) {
                    getroNewsprintAttributesStatus.postValue(GetUserNewsprintAttributesApiStatus.NoData)
                    return
                }

                getroNewsprintAttributesStatus.postValue(
                    GetUserNewsprintAttributesApiStatus.Success(
                        value,
                    ),
                )

                val applicationContext = FlagshipApplication.getInstance().applicationContext
                val checkPrefLmt = PrefUtils.getNewsprintAttributesLmt(applicationContext)

                PrefUtils.setNewsprintAttributesLmt(
                    applicationContext,
                    lastUpdated.coerceAtLeast(checkPrefLmt),
                )
                setNewsprintEngagedStatus(value.body?.engagedStatus ?: "")
                setNewsprintReaderType(value.body?.readerType ?: "")
            }
                ?: getroNewsprintAttributesStatus.postValue(GetUserNewsprintAttributesApiStatus.NewUser) // null response indicates user has never seen content packs before
        }

        private suspend fun processNewsprintStatePreferenceValueItem(
            newsprintStateValueItem: NewsprintStatePreferenceValueItem,
            lastUpdated: Long,
        ) {
            newsprintStateValueItem.value?.let { value: NewsprintStateValueItem ->

                if (value.period?.trim()?.equals(PERIOD, true) == false) {
                    getroNewsprintStateStatus.postValue(GetUserNewsprintStateApiStatus.NoData)
                    return
                }

                getroNewsprintStateStatus.postValue(
                    GetUserNewsprintStateApiStatus.Success(
                        value,
                    ),
                )

                val applicationContext = FlagshipApplication.getInstance().applicationContext
                val checkPrefLmt = PrefUtils.getNewsprintStateLmt(applicationContext)

                PrefUtils.setNewsprintStateLmt(
                    applicationContext,
                    lastUpdated.coerceAtLeast(checkPrefLmt),
                )
                setNewsprintHasViewed(value.body?.hasViewedNewsprint ?: false)
                setNewsprintReaderType(value.body?.readerType ?: "")
            }
                ?: getroNewsprintStateStatus.postValue(GetUserNewsprintStateApiStatus.NewUser) // null response indicates user has never seen content packs before
        }

        companion object {
            const val NS_GET_FAILURE = "newsprint=get"
            private const val PERIOD = "2024YTD"
        }
    }
