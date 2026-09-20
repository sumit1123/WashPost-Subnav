/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.sections.viewmodels.sectionsribbon

import com.wapo.flagship.features.sections.model.Section

data class SectionsRibbonUiState(
    val sections: List<Section> = listOf(),
    val selectedSectionIndex: Int = 0,
    val visitedNewsprintSection: Boolean = false,
    val isVisible: Boolean = true,
    val isLowDataModeEnable: Boolean = false,
    val shouldShowCustomNav: Boolean = true
)
