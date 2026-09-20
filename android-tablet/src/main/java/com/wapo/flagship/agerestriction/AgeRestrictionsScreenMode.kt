/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.agerestriction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class AgeRestrictionsScreenMode: Parcelable {
    data object UnknownBlockAgeRestriction: AgeRestrictionsScreenMode()
    data object BlockAgeRestriction: AgeRestrictionsScreenMode()
    data object ParentPermissionsBlockRestriction: AgeRestrictionsScreenMode()
    data object VisitPlayStoreBlockRestriction: AgeRestrictionsScreenMode()
}
