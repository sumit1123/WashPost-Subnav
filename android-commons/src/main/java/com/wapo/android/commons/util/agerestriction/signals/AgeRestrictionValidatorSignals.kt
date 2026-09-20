/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.commons.util.agerestriction.signals

import android.app.Activity
import com.google.android.play.agesignals.AgeSignalsAccessRequest
import com.google.android.play.agesignals.AgeSignalsManager
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.model.AgeSignalsStatus
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.agerestriction.AgeRestrictionValidator
import com.wapo.android.commons.util.agerestriction.AgeRestrictionsError
import com.wapo.android.commons.util.agerestriction.AgeState
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeHelper
import com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper.AgeRestrictionsFakeSetUp

class AgeRestrictionValidatorSignals: AgeRestrictionValidator() {

    override suspend fun isAgeEligible(
        activity: Activity,
        ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp?,
        onResult: (AgeState) -> Unit,
    ) {
        Logger.d(TAG, "isAgeEligible")

        val fakeHelper = AgeRestrictionsFakeHelper(ageRestrictionsFakeSetUp)
        var ageSignalsManager = AgeSignalsManagerFactory.create(activity.applicationContext)
        val accessRequest = AgeSignalsAccessRequest.builder()
            .setActivity(activity)
            .build()

        if (fakeHelper.isErrorFake()) {
            onResult(AgeState.Error(error = fakeHelper.getErrorFake()))
        } else {
            if (ageRestrictionsFakeSetUp?.fakeAgeSignalsManagerEnable == true) {
                ageSignalsManager = fakeHelper.getFakeAgeSignalsManager() ?: ageSignalsManager
            }

            ageSignalsManager.requestAgeSignalsAccess(accessRequest)
                .addOnSuccessListener { accessResult ->
                    Logger.d(TAG, "accessResult => $accessResult")

                    val ageSignalsStatus = accessResult.ageSignalsStatus()

                    when (ageSignalsStatus) {
                        AgeSignalsStatus.SHARED -> {
                            retrieveAgeSignals(ageSignalsManager, onResult)
                        }
                        AgeSignalsStatus.UNSPECIFIED -> {
                            onResult(AgeState.Unknown)
                        }
                        AgeSignalsStatus.VERIFICATION_REQUIRED -> {
                            /*
                            The user's age is unknown and the user is in an applicable jurisdiction or region where age verification and age signals sharing is mandatory.
                            To obtain an age signal from Google Play in these regions, ask the user to visit the Play Store to resolve their status.
                             */
                            onResult(AgeState.VisitPlayStore)
                        }
                        AgeSignalsStatus.NOT_SHARED -> {
                            // In a mandatory-sharing jurisdiction Play returns VERIFICATION_REQUIRED.
                            // NOT_SHARED therefore means age signals aren't available to this app, which
                            // includes users outside a regulated region or in optional-sharing regions.
                            onResult(AgeState.Success)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle API/Play Store connection and system errors
                    onResult(AgeState.Error(AgeRestrictionsError.fromErrorMessage(exception.message.toString())))
                }
        }
    }

    private fun retrieveAgeSignals(
        manager: AgeSignalsManager,
        onResult: (AgeState) -> Unit,
    ) {
        // 3. Perform the actual age signals query once sharing is active.
        manager.checkAgeSignals(AgeSignalsRequest.builder().build())
            .addOnSuccessListener { ageSignalsResult ->
                Logger.d(TAG, "ageSignalsResult => $ageSignalsResult")

                val ageLower = ageSignalsResult.ageLower()
                val ageUpper = ageSignalsResult.ageUpper()
                val significantChangeStatus = ageSignalsResult.significantChangeStatus()

                onResult(validateAge(ageLower, ageUpper, significantChangeStatus))
            }
            .addOnFailureListener { exception ->
                onResult(AgeState.Error(AgeRestrictionsError.fromErrorMessage(exception.message.toString())))
            }
    }

    override fun cleanValidator() {}

    private companion object {
        const val TAG = "AgeRestrictionValidatorSignals"
    }
}
