package com.wapo.flagship.features.preferencesapi.repo

import android.content.Context
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.preferencesapi.models.SnoozeInfo
import com.wapo.flagship.features.preferencesapi.models.WallDismissalSetRequest
import com.wapo.flagship.features.preferencesapi.models.WallDismissalGetResponse
import com.wapo.flagship.features.preferencesapi.models.WallDismissalSetResponse
import com.wapo.flagship.features.preferencesapi.services.PreferencesApiService
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.paywall.auth.AuthHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WallDismissalRepo @Inject constructor(
    @ApplicationContext val context: Context,
    private val preferencesApiService: PreferencesApiService,
    private val dispatcherProvider: DispatcherProvider
) : BasePreferencesRepo() {

    suspend fun getWallDismissal() {
        withContext(dispatcherProvider.io) {
            AuthHelper.getInstance(context).runWithValidToken {
                CoroutineScope(dispatcherProvider.io).launch {
                    try {
                        val result = preferencesApiService.getWallDismissal(getHeaders())
                        processGetWallDismissal(result)
                    } catch (e: Exception) {
                        val builder: EventLog.Builder = EventLog.Builder()
                        builder
                            .setMessage("getWallDismissal request failed")
                            .setModule(LogModules.PREFERENCES)
                            .setErrorMessage(e.message)
                            .set("cause", e.cause)
                        RemoteLog.e(context, builder.build())
                    }
                }
            }
        }
    }

    private fun processGetWallDismissal(result: APIResult<WallDismissalGetResponse>) {
        when (result) {
            is APIResult.Failure, is APIResult.NetworkError -> {
                val builder: EventLog.Builder = EventLog.Builder()
                builder
                    .setMessage("getWallDismissal request failed")
                    .setModule(LogModules.PREFERENCES)
                    .setErrorMessage(result.getMessage())
                RemoteLog.e(context, builder.build())
            }
            is APIResult.Success -> {
                val prefValue = result.data?.preferenceValues?.firstOrNull()
                val wallDismissal = prefValue?.value
                PrefUtils.setWallDismissal(context, wallDismissal)
                val remoteLu = prefValue?.lastUpdated ?: PrefUtils.getCheckPrefLmt(context)
                PrefUtils.setWallDismissalLmt(context, remoteLu)
                PreferencesSyncCoordinator.updateFromRemote(
                    context,
                    PreferencesSyncCoordinator.WALL_DISMISSAL,
                    remoteLu
                )
            }
        }
    }

    suspend fun setWallDismissal(wallDismissal: Map<String, Map<String, SnoozeInfo?>?>?) {
        withContext(dispatcherProvider.io) {
            AuthHelper.getInstance(context).runWithValidToken {
                CoroutineScope(dispatcherProvider.io).launch {
                    try {
                        PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.WALL_DISMISSAL)
                        val result = preferencesApiService.setWallDismissal(
                            getHeaders(),
                            WallDismissalSetRequest(wallDismissal)
                        )
                        processSetWallDismissal(result)
                    } catch (e: Exception) {
                        val builder: EventLog.Builder = EventLog.Builder()
                        builder
                            .setMessage("setWallDismissal request failed")
                            .setModule(LogModules.PREFERENCES)
                            .setErrorMessage(e.message)
                            .set("cause", e.cause)
                        RemoteLog.e(context, builder.build())
                    }
                }
            }
        }
    }

    private fun processSetWallDismissal(result: APIResult<WallDismissalSetResponse>) {
        when (result) {
            is APIResult.Failure, is APIResult.NetworkError -> {
                val builder: EventLog.Builder = EventLog.Builder()
                builder
                    .setMessage("setWallDismissal request failed")
                    .setModule(LogModules.PREFERENCES)
                    .setErrorMessage(result.getMessage())
                RemoteLog.e(context, builder.build())
            }
            is APIResult.Success -> {
                val wallDismissal = result.data?.preferenceValue?.value
                PrefUtils.setWallDismissal(context, wallDismissal)
                val checkPrefLmt = PrefUtils.getCheckPrefLmt(context)
                PrefUtils.setWallDismissalLmt(context, checkPrefLmt)
                val lu = result.data?.preferenceValue?.lastUpdated ?: checkPrefLmt
                PreferencesSyncCoordinator.updateFromRemote(
                    context,
                    PreferencesSyncCoordinator.WALL_DISMISSAL,
                    lu
                )
            }
        }
    }

    companion object {
        private val TAG = WallDismissalRepo::class.java.simpleName
    }
}
