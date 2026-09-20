package com.wapo.flagship.navigation.viewmodel.sectionnav

import com.wapo.flagship.json.MenuSection

sealed class SectionNavEvent {

    data class OpenSection(
        val menuSection: MenuSection,
        val navType: String? = null
    ): SectionNavEvent()

    object OpenPrint: SectionNavEvent()

    object ReloadTitle: SectionNavEvent()

    data class OpenInWebView(val url: String): SectionNavEvent()
}
