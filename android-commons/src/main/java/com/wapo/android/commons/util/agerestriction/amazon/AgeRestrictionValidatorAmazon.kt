/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.commons.util.agerestriction.amazon

import android.app.Activity
import android.content.Context
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeSetUp
import com.wapo.android.commons.util.agerestriction.AgeRestrictionValidator
import com.wapo.android.commons.util.agerestriction.AgeRestrictionsError
import com.wapo.android.commons.util.agerestriction.AgeState
import com.wapo.android.commons.util.agerestriction.amazon.model.ResponseStatus
import com.wapo.android.commons.util.agerestriction.amazon.model.UserStatus
import com.wapo.android.commons.util.agerestriction.amazon.model.getSaveResponseStatus
import com.wapo.android.commons.util.agerestriction.amazon.model.getSaveUserStatus

class AgeRestrictionValidatorAmazon(context: Context): AgeRestrictionValidator() {

    private var amazonUserDataClient: AmazonUserDataClient? = AmazonUserDataClient(context)

    override suspend fun isAgeEligible(
        activity: Activity,
        ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp?,
        onResult: (AgeState) -> Unit,
    ) {
        val data = amazonUserDataClient?.getMyData()

        if (data != null) {
            val responseStatusValue = getSaveResponseStatus(data.responseStatus)
            val result = when (responseStatusValue) {
                ResponseStatus.SUCCESS -> {
                    val userStatusValue = getSaveUserStatus(data.userStatus)

                    when (userStatusValue) {
                        UserStatus.VERIFIED -> AgeState.Success
                        UserStatus.SUPERVISED -> AgeState.Success
                        UserStatus.CONSENT_NOT_GRANTED -> AgeState.ParentPermission
                        UserStatus.UNKNOWN -> AgeState.Unknown
                    }
                }

                ResponseStatus.APP_NOT_OWNED -> AgeState.Error(AgeRestrictionsError.AmazonAppNotOwned)
                ResponseStatus.INTERNAL_TRANSIENT_ERROR -> AgeState.Error(AgeRestrictionsError.ClientTransientError)
                ResponseStatus.INTERNAL_ERROR -> AgeState.Error(AgeRestrictionsError.InternalError)
                ResponseStatus.FEATURE_NOT_SUPPORTED -> AgeState.Error(AgeRestrictionsError.AmazonFeaturedNotSupportedError)
            }
            onResult(result)
        } else {
            onResult(AgeState.Success)
        }
    }

    override fun cleanValidator() {
        amazonUserDataClient?.clean()
        amazonUserDataClient = null
    }
}
