/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.splash

import com.wapo.android.commons.logger.R

data class SplashModel(
    val dismissSplash: Boolean = false,
    val navigateToBlockAgeRestriction: Boolean = false,
    val navigateToParentPermissionsBlockRestriction: Boolean = false,
    val navigateToUnknownState: Boolean = false,
    val navigateToVisitPlayStore: Boolean = false,
    val showModalAgeRestrictionUpgrade: Boolean = false,
    val showModalAgeRestrictionError: Boolean = false,
    val showModalAgeRestrictionErrorTitle: Int = R.string.age_restriction_app_dialog_message_error_title,
    val showModalAgeRestrictionErrorMessage: Int = R.string.age_restriction_error_unknownerror_message,
    val loadingAgeRestriction: Boolean = true,
    val retryCall: Boolean = false,
    val blockUser: Boolean = false
)
