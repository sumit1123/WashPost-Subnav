/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.domain.repository

import com.washingtonpost.android.paywall.auth.AuthEntryPoint

interface OnboardingRepo {

    fun getStepSize(): Int

    fun getCurrentStep(): Int

    fun loadPersonalizePages(authEntryPoint: AuthEntryPoint? = null)

    fun loadAlertsPageIfNeeded()

    fun loadContentPacksPageIfNeeded()

    fun loadAudioPageIfNeeded()

    fun getCurrentPageId(): Int?

    fun isCurrentScreenAlreadySeen(): Boolean

    fun addSeenPage(pageId: Int)

    fun nextPage()

    fun previousPage()

    fun isAlreadyShown(): Boolean

    fun setShown()

    fun shouldShowSteps(): Boolean

    fun shouldShowBack(): Boolean

    fun isFirstPage(): Boolean

    fun setAlertsContinueActive(value: Boolean)

    fun setContentPacksContinueActive(value: Boolean)

    fun getAlertsContinueActive(): Boolean

    fun getContentPacksContinueActive(): Boolean
}
