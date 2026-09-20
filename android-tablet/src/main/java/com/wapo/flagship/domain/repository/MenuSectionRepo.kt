package com.wapo.flagship.domain.repository

import com.wapo.flagship.IntentHelper
import com.wapo.flagship.json.MenuSection

interface MenuSectionRepo {

    fun getMenuSections(): List<MenuSection>?

    fun getMenuSectionFromUrl(
        url: String?,
        intentHelper: IntentHelper,
        menuSections: List<MenuSection>?,
    ): MenuSection?
}
