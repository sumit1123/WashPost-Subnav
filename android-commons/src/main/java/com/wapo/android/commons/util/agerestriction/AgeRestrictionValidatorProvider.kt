package com.wapo.android.commons.util.agerestriction

import android.content.Context
import android.os.Build
import com.wapo.android.commons.util.Utils
import com.wapo.android.commons.util.agerestriction.amazon.AgeRestrictionValidatorAmazon
import com.wapo.android.commons.util.agerestriction.signals.AgeRestrictionValidatorSignals

object AgeRestrictionValidatorProvider {

    fun provideAgeRestrictionValidator(context: Context): AgeRestrictionValidator {
        return if (Utils.isAmazonBuild()) {
            AgeRestrictionValidatorAmazon(context)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AgeRestrictionValidatorSignals()
        } else {
            AgeRestrictionValidatorOldVersions()
        }
    }
}
