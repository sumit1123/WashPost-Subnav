/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.sections.viewmodels.sectionsribbon

import com.wapo.flagship.features.grid.Tracking

sealed class SectionsRibbonEvents {

    data class SelectedSectionIndexEvent(val id: String): SectionsRibbonEvents()
    data class VisitedNewsprintSectionEvent(val visited: Boolean): SectionsRibbonEvents()
    data class OpenSectionOnLaunchEvent(val open: Boolean): SectionsRibbonEvents()
    data class SendTrackingInfoEvent(val tracking: Tracking): SectionsRibbonEvents()
    object RibbonReadyEvent: SectionsRibbonEvents()
}
