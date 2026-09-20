package com.wapo.android.commons.util.agerestriction

import android.app.Activity
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeSetUp

class AgeRestrictionValidatorOldVersions: AgeRestrictionValidator() {

    override suspend fun isAgeEligible(
        activity: Activity,
        ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp?,
        onResult: (AgeState) -> Unit,
    ) {
        onResult(AgeState.RequireUpdate)
    }

    override fun cleanValidator() {}
}
