package com.wapo.flagship.data.repository

import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.domain.repository.ContentManagerDataRepo
import com.wapo.flagship.domain.repository.MenuSectionRepo
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.json.MenuSection
import javax.inject.Inject

class MenuSectionRepoImpl @Inject constructor(
    private val contentManagerDataRepo: ContentManagerDataRepo,
): MenuSectionRepo {
    override fun getMenuSections(): List<MenuSection>? = contentManagerDataRepo.getMenuSections()

    override fun getMenuSectionFromUrl(
        url: String?,
        intentHelper: IntentHelper,
        menuSections: List<MenuSection>?
    ): MenuSection? {
        if (url != null && menuSections != null) {
            val urlParser = URLParser(url)
            if (!DeepLinksProcessor.isSectionPath(urlParser)) {
                return null
            }
            val sectionId = DeepLinksProcessor.getSectionId(urlParser)
            return intentHelper.findMenuSection(sectionId, menuSections)
        }
        return null
    }
}
