/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.commons.util.agerestriction.fakeagerestrictionshelper

data class AgeRestrictionsFakeSetUp(
    val fakeAgeSignalsManagerEnable: Boolean = false,
    val errorAPI: String = "",
    val userUpperAge: String = "",
    val userLowerAge: String = "",
    val accessStatus: String = "",
    val significantChangeStatus: String = ""
)

val AgeSignalsV004AccessStatuses = listOf(
    "SHARED",
    "NOT_SHARED",
    "VERIFICATION_REQUIRED",
    "UNSPECIFIED"
)

val AgeSignalsV004SignificantChangeStatuses = listOf(
    "UNSPECIFIED",
    "APPROVED",
    "PENDING",
    "DECLINED"
)
