package com.wapo.flagship.data.repository

import com.wapo.flagship.content.ContentManager
import com.wapo.flagship.domain.repository.ContentManagerDataRepo
import com.wapo.flagship.json.MenuSection
import javax.inject.Inject

class ContentManagerDataRepoImpl @Inject constructor(
    private val contentManager: ContentManager
): ContentManagerDataRepo {

    override fun getMenuSections(): List<MenuSection>? {
        return contentManager.allMenuSections
            .toBlocking()
            .firstOrDefault(null)
    }
}
