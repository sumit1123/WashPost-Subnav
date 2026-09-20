/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper

import com.google.android.play.agesignals.AgeSignalsAccessResult
import com.google.android.play.agesignals.AgeSignalsResult
import com.google.android.play.agesignals.model.AgeSignalsStatus
import com.google.android.play.agesignals.model.SignificantChangeStatus
import com.google.android.play.agesignals.testing.FakeAgeSignalsManager
import com.wapo.android.commons.util.agerestriction.AgeRestrictionsError

class AgeRestrictionsFakeHelper(private val ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp?) {

    fun isErrorFake() = ageRestrictionsFakeSetUp?.fakeAgeSignalsManagerEnable == true && ageRestrictionsFakeSetUp.errorAPI.isNotEmpty()

    fun getErrorFake(): AgeRestrictionsError {
        val apiError = ageRestrictionsFakeSetUp?.errorAPI?.isNotEmpty() == true

        return if (apiError) {
            val textCode = ageRestrictionsFakeSetUp.errorAPI
            val code = AgeRestrictionsError.extractCode(textCode)
            AgeRestrictionsError.codeToErrorMap[code] ?: AgeRestrictionsError.UnknownError
        } else {
            AgeRestrictionsError.UnknownError
        }
    }

    fun getFakeAgeSignalsManager(): FakeAgeSignalsManager? {
        return ageRestrictionsFakeSetUp?.let {
            val fakeVerifiedUser = getFakeVerifiedUser(ageRestrictionsFakeSetUp)
            val ageSignalsManager = FakeAgeSignalsManager()
            ageSignalsManager.setNextAgeSignalsAccessResult(getFakeAccessResult(ageRestrictionsFakeSetUp))
            ageSignalsManager.setNextAgeSignalsResult(
                fakeVerifiedUser
            )
            ageSignalsManager
        }
    }

    private fun getFakeVerifiedUser(ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp): AgeSignalsResult {

        val builder = AgeSignalsResult.builder()

        setFakeAgeRange(ageRestrictionsFakeSetUp, builder)
        setFakeSignificantChangeStatus(builder)

        return builder.build()
    }

    private fun getFakeAccessResult(setUp: AgeRestrictionsFakeSetUp): AgeSignalsAccessResult {
        val status = when (setUp.accessStatus) {
            "SHARED" -> AgeSignalsStatus.SHARED
            "NOT_SHARED" -> AgeSignalsStatus.NOT_SHARED
            "VERIFICATION_REQUIRED" -> AgeSignalsStatus.VERIFICATION_REQUIRED
            else -> AgeSignalsStatus.UNSPECIFIED
        }
        return AgeSignalsAccessResult.builder()
            .setAgeSignalsStatus(status)
            .build()
    }

    private fun setFakeSignificantChangeStatus(builder: AgeSignalsResult.Builder) {
        val status = when (ageRestrictionsFakeSetUp?.significantChangeStatus) {
            "APPROVED" -> SignificantChangeStatus.APPROVED
            "PENDING" -> SignificantChangeStatus.PENDING
            "DECLINED" -> SignificantChangeStatus.DECLINED
            else -> SignificantChangeStatus.UNSPECIFIED
        }
        builder.setSignificantChangeStatus(status)
    }

    private fun setFakeAgeRange(ageRestrictionsFakeSetUp: AgeRestrictionsFakeSetUp, builder: AgeSignalsResult.Builder) {
        val intUpperAge = ageRestrictionsFakeSetUp.userUpperAge.toIntOrNull()
        builder.setAgeUpper(intUpperAge)

        val intLowerAge = ageRestrictionsFakeSetUp.userLowerAge.toIntOrNull()
        builder.setAgeLower(intLowerAge)
    }
}
