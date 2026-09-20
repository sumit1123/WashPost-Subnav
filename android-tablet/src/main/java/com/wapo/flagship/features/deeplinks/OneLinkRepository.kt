package com.wapo.flagship.features.deeplinks

import com.squareup.moshi.Moshi
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.features.deeplinks.models.OneLinkRequest
import com.washingtonpost.userhistory.network.APIResult
import javax.inject.Inject

private const val TAG = "OneLinkRepository"

class OneLinkRepository @Inject constructor(
    private val oneLinkService: OneLinkService,
    private val remoteLogRepo: RemoteLogRepo,
) {
    private val moshi = Moshi.Builder().build()
    private val requestAdapter = moshi.adapter(OneLinkRequest::class.java)

    suspend fun generateAudioOneLink(
        subtype: String,
        id: String
    ): OneLinkGeneratedStatus {
        val request = OneLinkRequest(
            type = "audio",
            subtype = subtype,
            id = id
        )
        val jsonPayload = requestAdapter.toJson(request)
        val response = oneLinkService.generateOneLink(jsonPayload)
        return when (response) {
            is APIResult.Failure -> {
                val message = "Error generating one link"
                Logger.d(
                    TAG,
                    "$message: Status Code ${response.statusCode} | Message: ${response.rawResponse}"
                )
                val eventLog = EventLog.Builder()
                    .setMessage(message)
                    .setModule(LogModules.DEEPLINK)
                    .set("subtype", subtype)
                    .set("id", id)
                    .setErrorCode(response.statusCode)
                    .setErrorMessage(response.rawResponse)
                    .setForceUpload()
                    .build()

                remoteLogRepo.e(eventLog)
                OneLinkGeneratedStatus.Error
            }

            is APIResult.NetworkError -> {
                OneLinkGeneratedStatus.Error
            }

            is APIResult.Success -> {
                val oneLink = response.data?.oneLink
                if (oneLink != null) {
                    OneLinkGeneratedStatus.Success(oneLink)
                } else {
                    Logger.d(TAG, "OneLink generation returned a success but link was null")
                    OneLinkGeneratedStatus.Error
                }
            }
        }
    }
}

sealed class OneLinkGeneratedStatus {
    data class Success(val oneLink: String) : OneLinkGeneratedStatus()
    data object Error : OneLinkGeneratedStatus()
}
