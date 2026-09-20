// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.aixp

import com.washingtonpost.android.config.domain.manager.ConfigManager

object SummariesFeatureFlag {

    fun enabled(): Boolean =
        ConfigManager.getInstance().config.summariesConfig.enabled

    fun realtimeSummaryEnabled(): Boolean =
        ConfigManager.getInstance().config.summariesConfig.realtimeSummary.enabled

    fun realtimeFeedbackEnabled(): Boolean =
        realtimeSummaryEnabled() && ConfigManager.getInstance().config.summariesConfig.realtimeFeedback.enabled

    fun shouldEnableSummariesIcon(approvedSummaryAvailable: Boolean, showFallbackSummary: Boolean): Boolean =
        enabled() && (approvedSummaryAvailable || (realtimeSummaryEnabled() && showFallbackSummary))
}
