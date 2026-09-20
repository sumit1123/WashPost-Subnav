package com.wapo.flagship.domain.repository

import com.wapo.flagship.json.MenuSection

interface ContentManagerDataRepo {

    fun getMenuSections(): List<MenuSection>?
}
