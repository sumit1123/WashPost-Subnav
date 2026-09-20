/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.commons.util.agerestriction

import android.app.Activity
import com.google.android.play.agesignals.model.SignificantChangeStatus
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeSetUp

abstract class AgeRestrictionValidator {

    abstract suspend fun isAgeEligible(
        activity: Activity,
        ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp? = null,
        onResult: (AgeState) -> Unit,
    )
    abstract fun cleanValidator()

    protected fun validateAge(
        ageLower: Int?,
        ageUpper: Int?,
        significantChangeStatus: Int? = null,
    ): AgeState {
        if (ageLower == null || (ageUpper != null && ageUpper < ageLower)) {
            return AgeState.Unknown
        }

        return when {
            ageLower < LOWER_AGE -> AgeState.Restricted
            ageLower < UPPER_AGE -> when (significantChangeStatus) {
                SignificantChangeStatus.PENDING,
                SignificantChangeStatus.DECLINED -> AgeState.ParentPermission
                else -> AgeState.Success
            }
            else -> AgeState.Success
        }
    }

    companion object {
        const val LOWER_AGE = 13
        const val UPPER_AGE = 18
    }
}

sealed class AgeState() {
    data object Success: AgeState()
    data object Restricted: AgeState()
    data object RequireUpdate: AgeState()
    data object ParentPermission: AgeState()
    data object Unknown: AgeState()
    data object VisitPlayStore: AgeState()
    data class Error(val error: AgeRestrictionsError): AgeState()
}
