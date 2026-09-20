/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.commons.util.agerestriction

import com.wapo.android.commons.logger.R

sealed class AgeRestrictionsError(
    val code: Int,
    val error: Int,
    val title: Int = R.string.age_restriction_app_dialog_message_error_title_default,
    val block: Boolean = true
) {

    data object ApiNotAvailable : AgeRestrictionsError(
        code = -1,
        error = R.string.age_restriction_error_playstoreversionoutdated_message,
        title = R.string.age_restriction_app_dialog_message_update_title
    )

    data object AppNotOwned : AgeRestrictionsError(
        code = -9,
        error = R.string.age_restriction_error_appnotowned_message,
        title = R.string.age_restriction_error_appnotowned_title,
    )

    data object CannotBindToService : AgeRestrictionsError(
        code = -5,
        error = R.string.age_restriction_error_playstoreversionoutdated_message
    )

    data object ClientTransientError : AgeRestrictionsError(
        code = -8,
        error = R.string.age_restriction_error_clienttransienterror_message
    )

    data object InternalError : AgeRestrictionsError(
        code = -100,
        error = R.string.age_restriction_error_internalerror_message
    )

    data object NetworkError : AgeRestrictionsError(
        code = -3,
        error = R.string.age_restriction_error_networkerror_message
    )

    data object PlayServicesNotFound : AgeRestrictionsError(
        code = -4,
        error = R.string.age_restriction_error_playservicesnotfound_message,
        title = R.string.age_restriction_app_dialog_message_error_title
    )

    data object PlayServicesVersionOutdated : AgeRestrictionsError(
        code = -7,
        error = R.string.age_restriction_error_playservicesversionoutdated_message,
        title = R.string.age_restriction_app_dialog_message_update_title
    )

    data object PlayStoreNotFound : AgeRestrictionsError(
        code = -2,
        error = R.string.age_restriction_error_playstorenotfound_message_require,
        title = R.string.age_restriction_app_dialog_message_error_title
    )

    data object PlayStoreVersionOutdated : AgeRestrictionsError(
        code = -6,
        error = R.string.age_restriction_error_playstoreversionoutdated_message,
        title = R.string.age_restriction_app_dialog_message_update_title
    )

    data object SdkVersionOutdated : AgeRestrictionsError(
        code = -10,
        error = R.string.age_restriction_error_sdkversionoutdated_message,
        title = R.string.age_restriction_app_dialog_message_update_title
    )

    data object UnknownError : AgeRestrictionsError(
        code = -999,
        error = R.string.age_restriction_error_unknownerror_message
    )

    data object AmazonAppNotOwned : AgeRestrictionsError(
        code = -998,
        error = R.string.age_restriction_error_amazonappnotowned_message,
        title = R.string.age_restriction_app_dialog_message_error_amazon_title
    )

    data object AmazonFeaturedNotSupportedError : AgeRestrictionsError(
        code = -997,
        error = R.string.age_restriction_error_amazonfeaturednotsupportederror_message,
        title = R.string.age_restriction_app_dialog_message_error_amazon_title
    )

    companion object {

        val codeToErrorMap: Map<Int, AgeRestrictionsError> by lazy {
            listOf(
                ApiNotAvailable,
                AppNotOwned,
                CannotBindToService,
                ClientTransientError,
                InternalError,
                NetworkError,
                PlayServicesNotFound,
                PlayServicesVersionOutdated,
                PlayStoreNotFound,
                PlayStoreVersionOutdated,
                SdkVersionOutdated,
                AmazonAppNotOwned,
                AmazonFeaturedNotSupportedError,
                UnknownError
            ).associateBy { it.code }
        }

        fun extractCode(input: String): Int {
            // Regex breakdown:
            // [+-]? matches an optional plus or minus sign
            // \d+ matches one or more consecutive digits
            val regex = """[+-]?\d+""".toRegex()

            val matchResult = regex.find(input)

            // If a match is found, convert it to an Int; otherwise return UnknownError.code
            return matchResult?.value?.toIntOrNull() ?: UnknownError.code
        }

        fun fromErrorMessage(errorMessage: String): AgeRestrictionsError {
            val code = extractCode(errorMessage)
            return codeToErrorMap[code] ?: UnknownError
        }
    }
}
