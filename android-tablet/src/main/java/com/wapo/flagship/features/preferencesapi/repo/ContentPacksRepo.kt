// Copyright (c) 2022 The Washington Post. All rights reserved.

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
import com.wapo.android.commons.constants.IF_MODIFIED_SINCE
import com.wapo.android.commons.constants.LAST_MODIFIED
import com.wapo.android.commons.constants.OS_VERSION
import com.wapo.android.commons.constants.REQUEST_ID
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.preferencesapi.ContentPacksListApiStatus
import com.wapo.flagship.features.preferencesapi.GetUserContentPacksApiStatus
import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem
import com.wapo.flagship.features.preferencesapi.models.ContentPacksGetResponse
import com.wapo.flagship.features.preferencesapi.models.ContentPacksSetRequest
import com.wapo.flagship.features.preferencesapi.models.ContentPacksSetResponse
import com.wapo.flagship.features.preferencesapi.models.ContentPacksUiResponseBody
import com.wapo.flagship.features.preferencesapi.models.ContentPacksValueItem
import com.wapo.flagship.features.preferencesapi.models.Followable
import com.wapo.flagship.features.preferencesapi.models.SetUserContentPacksApiStatus
import com.wapo.flagship.features.preferencesapi.services.PreferencesApiService
import com.wapo.flagship.features.preferencesapi.services.PreferencesLocalStorageService
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.paywall.util.PaywallConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * A repository for accessing content pack service
 * Functions of this repo
 *  1. Get list of content packs the user has subscribed to
 *  2. Set list of content packs the user wants to subscribe to
 *  3. Get list of content packs data required for rendering screen.
 */
class ContentPacksRepo
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        private val preferencesApiService: PreferencesApiService,
        private val preferencesLocalStorageService: PreferencesLocalStorageService,
        private val dispatcherProvider: DispatcherProvider,
        private val remoteLogRepo: RemoteLogRepo,
    ) {
        /**
         * Mediator live-data to post[GetUserContentPacksApiStatus] to viewmodel
         */
        val getroContentPackStatus = LiveEvent<GetUserContentPacksApiStatus>()

        /**
         * Mediator live-data to post[SetUserContentPacksApiStatus] to viewmodel
         */
        val setroContentPackStatus = LiveEvent<SetUserContentPacksApiStatus>()

        /**
         * Mediator live-data to post[ContentPacksListApiStatus] to viewmodel
         * This is used for UI rendering
         */
        val getContentPacksListStatus = LiveEvent<ContentPacksListApiStatus>()

        suspend fun updateTopicsFollowed(
            context: Context,
            following: Boolean,
            contentPackId: String,
        ) = withContext(
            dispatcherProvider.io,
        ) {
            val list = PrefUtils.getSelectedContentPacks(context) ?: return@withContext
            val contentPackUiItem = getContentPackForId(contentPackId)
            if (following) {
                val item = list.find { it.pack != null && it.pack == contentPackId }
                if (item == null && contentPackUiItem != null) {
                    list.add(ContentPacksValueItem(true, contentPackId, contentPackUiItem.referenceId))
                }
            } else {
                val item = list.find { it.pack != null && it.pack == contentPackId }
                item?.let {
                    list.remove(it)
                }
            }
            PrefUtils.setSelectedContentPacks(context, list)
            setUserContentPacks(list)
        }

        private fun getFollowedTopics(context: Context): List<ContentPacksValueItem>? = PrefUtils.getSelectedContentPacks(context)

        fun isTopicFollowed(
            context: Context,
            packId: String,
        ): Boolean {
            val topicsFollowed = getFollowedTopics(context)
            topicsFollowed?.let {
                return it.find { it.pack != null && it.pack == packId } != null
            } ?: return false
        }

        /**
         * Start call to get list of content packs user has subscribed to.
         */
        suspend fun getUserContentPacks() =
            withContext(dispatcherProvider.io) {
                AuthHelper.getInstance(context).runWithValidToken {
                    CoroutineScope(dispatcherProvider.io).launch {
                        try {
                            val result = preferencesApiService.getContentPacks(getHeaders())
                            processGetContentPacks(result)
                        } catch (e: Exception) {
                            val builder: EventLog.Builder = EventLog.Builder()
                            builder
                                .setMessage("getUserContentPacks request failed")
                                .setModule(LogModules.PREFERENCES)
                                .setErrorMessage(e.message)
                                .set("cause", e.cause)
                            RemoteLog.e(context, builder.build())
                        }
                    }
                }
            }

    suspend fun setUserPersoPodConfig(body: RequestBody) {
        withContext(dispatcherProvider.io) {
            val result = preferencesApiService.setUserPersoPodConfig(getHeaders(), body)
            when (result) {
                is APIResult.Failure -> {
                    if (AppContextUtils.isConnectingOrConnected()) {
                        val eventLog = EventLog.Builder()
                            .setMessage("Set Perso Pod Config Failed")
                            .setModule(LogModules.PERSONALIZED_PODCASTS)
                            .setErrorCode(result.statusCode)
                            .setErrorMessage(result.rawResponse)
                            .setForceUpload()
                            .build()
                        remoteLogRepo.e(eventLog)
                    }
                }
                else -> {}
            }
        }
    }


        /**
         * Start call to set list of content packs user wants to subscribe to.
         * - Requires content pack list that user wants to subscribe to.
         * - Returns success and subscribed list in response
         */
        suspend fun setUserContentPacks(contentPacksList: List<ContentPacksValueItem>) =
            withContext(
                dispatcherProvider.io,
            ) {
                AuthHelper.getInstance(context).runWithValidToken {
                    CoroutineScope(dispatcherProvider.io).launch {
                        try {
                            PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.CONTENT_PACKS)
                            val requestBody = ContentPacksSetRequest(contentPacksList)
                            val result =
                                preferencesApiService.setContentPacks(getHeaders(), requestBody)
                            processSetContentPacks(result)
                        } catch (e: Exception) {
                            val builder: EventLog.Builder = EventLog.Builder()
                            builder
                                .setMessage("setUserContentPacks request failed")
                                .setModule(LogModules.PREFERENCES)
                                .setErrorMessage(e.message)
                                .set("cause", e.cause)
                            RemoteLog.e(context, builder.build())
                        }
                    }
                }
            }

        /**
         * Start call to get list of content pack details that will be required
         * for rendering the items on the screen.
         */
        suspend fun getContentPackItems(isOnboarding: Boolean) =
            withContext(dispatcherProvider.io) {
                try {
                    val timeSinceLastFetch =
                        System.currentTimeMillis() -
                            PrefUtils.getLastContentPackItemsFetch(
                                context,
                            )
                    if (timeSinceLastFetch > PaywallConstants.FOUR_HOURS_IN_MILLISECONDS) {
                        AuthHelper.getInstance(context).runWithValidToken {
                            CoroutineScope(dispatcherProvider.io).launch {
                                when (val result =
                                    preferencesApiService.getContentPackItems(getCPHeaders())) {
                                    is APIResult.Success -> processContentPackItemsFromRemote(
                                        result,
                                        isOnboarding
                                    )
                                    is APIResult.Failure -> {
                                        if (result.statusCode != 304) {
                                            val builder: EventLog.Builder = EventLog.Builder()
                                            builder
                                                .setMessage("getContentPackItems request failed")
                                                .setModule(LogModules.PREFERENCES)
                                                .setErrorMessage(result.getMessage())
                                            RemoteLog.e(context, builder.build())
                                        }
                                    }
                                    is APIResult.NetworkError -> {
                                        // Attempt to process from storage
                                        processContentPackItemsFromStorage()
                                    }
                                }
                            }
                        }
                    } else {
                        processContentPackItemsFromStorage()
                    }
                } catch (e: Exception) {
                    val builder: EventLog.Builder = EventLog.Builder()
                    builder
                        .setMessage("getContentPackItems request failed")
                        .setModule(LogModules.PREFERENCES)
                        .setErrorMessage(e.message)
                        .set("cause", e.cause)
                    RemoteLog.e(context, builder.build())
                }
            }

        /**
         * Process GET response for users subscribed content packs.
         * - Success -> post to live data the list of content packs
         * - Failure -> post to live data the failure
         */
        private fun processGetContentPacks(result: APIResult<ContentPacksGetResponse>) {
            when (result) {
                is APIResult.Failure, is APIResult.NetworkError ->
                    getroContentPackStatus.postValue(
                        GetUserContentPacksApiStatus.Failure("$CP_GET_FAILURE ${result.getMessage()}"),
                    )

                is APIResult.Success -> {
                    val data = result.data
                    when {
                        data?.status?.lowercase(Locale.US) == SUCCESS -> {
                            val contentPackPreference =
                                data.preferenceValues?.find { it?.id?.preference?.name == CONTENT_PACKS_NAME }
                            contentPackPreference?.value?.let {
                                val followedTopics = it.filter { topic -> topic?.interested == true }
                                PrefUtils.setSelectedContentPacks(
                                    context,
                                    followedTopics,
                                )
                                if (it.isNotEmpty()) {
                                    getroContentPackStatus.postValue(
                                        GetUserContentPacksApiStatus.Success(
                                            it,
                                        ),
                                    )
                                    val lastUpdated =
                                        result.data.preferenceValues
                                            ?.mapNotNull { preferenceValue ->
                                                preferenceValue?.lastUpdated
                                            }?.max() ?: 0L
                                    val checkPrefLmt =
                                        PrefUtils.getCheckPrefLmt(
                                            FlagshipApplication.getInstance().applicationContext,
                                        )
                                    val timestamp = lastUpdated.coerceAtLeast(checkPrefLmt)
                                    PreferencesSyncCoordinator.updateFromRemote(
                                        context,
                                        PreferencesSyncCoordinator.CONTENT_PACKS,
                                        timestamp
                                    )
                                } else {
                                    getroContentPackStatus.postValue(
                                        GetUserContentPacksApiStatus.NoUserContentPacks,
                                    ) // empty array indicates user selected none
                                }
                            }
                                ?: getroContentPackStatus.postValue(
                                    GetUserContentPacksApiStatus.NewUser,
                                ) // null response indicates user has never seen content packs before
                        }

                        else ->
                            getroContentPackStatus.postValue(
                                GetUserContentPacksApiStatus.Failure(
                                    "$CP_GET_FAILURE response=$data",
                                ),
                            )
                    }
                }
            }
        }

        /**
         * Process POST response for users subscribed content packs.
         * - Success -> post to live data the list of content packs
         * - Failure -> post to live data the failure
         */
        private fun processSetContentPacks(result: APIResult<ContentPacksSetResponse>) {
            when (result) {
                is APIResult.Failure, is APIResult.NetworkError ->
                    setroContentPackStatus.postValue(
                        SetUserContentPacksApiStatus.Failure("$CP_SET_FAILURE ${result.getMessage()}"),
                    )

                is APIResult.Success -> {
                    val data = result.data
                    when {
                        data?.status?.lowercase(Locale.US) == SUCCESS -> {
                            data.preferenceValue?.value?.let {
                                // Note that we can turn off all content packs where the list would be empty
                                setroContentPackStatus.postValue(
                                    SetUserContentPacksApiStatus.Success(it),
                                )
                                PrefUtils.setSelectedContentPacks(
                                    context,
                                    it,
                                )
                                val lastUpdated = data.preferenceValue.lastUpdated ?: System.currentTimeMillis()
                                PreferencesSyncCoordinator.updateFromRemote(
                                    context,
                                    PreferencesSyncCoordinator.CONTENT_PACKS,
                                    lastUpdated
                                )
                            } ?: setroContentPackStatus.postValue(
                                SetUserContentPacksApiStatus.Failure(
                                    "$CP_SET_FAILURE response=$data",
                                ),
                            )
                        }

                        else ->
                            setroContentPackStatus.postValue(
                                SetUserContentPacksApiStatus.Failure(
                                    "$CP_SET_FAILURE response=$data",
                                ),
                            )
                    }
                }
            }
        }

        /**
         * Process GET response for content pack details
         * - Success -> post to live data the list of content packs
         * - Failure -> post to live data the failure
         */
        private fun processContentPackItemsFromRemote(
            result: APIResult.Success<ContentPacksUiResponseBody>,
            isOnboarding: Boolean = false,
        ) {
            val data = result.data
            var sortedOrShuffledList =
                data
                    ?.contentPacks
                    ?.filter { it?.disabled != true }
                    ?.sortedWith { o1, o2 ->
                        if (o1?.priority == null || o2?.priority == null || o1.priority == o2.priority) {
                            return@sortedWith o1?.heading?.compareTo(o2?.heading ?: "") ?: 0
                        } else {
                            return@sortedWith o1.priority.compareTo(o2.priority)
                        }
                    }

            result.headers.get(LAST_MODIFIED)?.let {
                preferencesLocalStorageService.setLastModifiedSince(context, it)
            }

            if (isOnboarding) {
                sortedOrShuffledList =
                    sortedOrShuffledList?.filter { it?.onboardingDisabled != true }?.shuffled()
            }

            // TODO this is a hacky workaround to properly nullify invalid Follow objects.
            //  Ideally we have a custom moshi adapter that always nullifies invalid Follow objects - when processing from local AND remote

            sortedOrShuffledList?.let {
                PrefUtils.setLastContentPackItemsFetch(
                    context,
                    System.currentTimeMillis(),
                )
                preferencesLocalStorageService.setContentPackItems(
                    context,
                    ContentPacksUiResponseBody(it),
                )
                val contentPacksFromLocal =
                    preferencesLocalStorageService.getContentPackItems(context)
                contentPacksFromLocal?.let {
                    getContentPacksListStatus.postValue(ContentPacksListApiStatus.Success(it))
                }
                    ?: getContentPacksListStatus.postValue(
                        ContentPacksListApiStatus.Failure(
                            "$CP_UI_GET_FAILURE response=$data",
                        ),
                    )
            }
                ?: getContentPacksListStatus.postValue(
                    ContentPacksListApiStatus.Failure("$CP_UI_GET_FAILURE response=$data"),
                )
        }

        private fun processContentPackItemsFromStorage() {
            val contentPackItems =
                preferencesLocalStorageService.getContentPackItems(context)
            contentPackItems?.let {
                getContentPacksListStatus.postValue(ContentPacksListApiStatus.Success(contentPackItems))
            } ?: run {
                getContentPacksListStatus.postValue(
                    ContentPacksListApiStatus.Failure(
                        "Unable to retrieve content pack items from storage",
                    ),
                )
            }
        }

        fun getContentPackForId(id: String): ContentPackUiItem? =
            preferencesLocalStorageService.getContentPackForId(
                context,
                id,
            )

        fun getFollowableForId(id: String): Followable? =
            preferencesLocalStorageService.getFollowableForId(
                context,
                id,
            )

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

        private fun getCPHeaders(): HashMap<String, String> {
            val headers = getBaseHeaders()
            preferencesLocalStorageService.getLastModifiedSince(context)?.let {
                headers[IF_MODIFIED_SINCE] = it
            }
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

        companion object {
            const val SUCCESS = "success"
            const val FAILURE = "failure"
            const val CONTENT_PACKS_NAME = "content-packs"
            const val CP_SET_FAILURE = "content_packs=set"
            const val CP_GET_FAILURE = "content_packs=get"
            const val CP_UI_GET_FAILURE = "content_packs=ui_get"
        }
    }
