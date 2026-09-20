/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.use_cases

import com.wapo.flagship.features.section.models.ArticleMeta
import com.wapo.flagship.data.WearDataManager
import com.wapo.flagship.features.section.repo.SectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 *  Transforms the flow of Result<PageBuilderAPIResponse>
 *  to Result<List<ArticleMeta>>.
 *
 */
class GetPageBuilderSection(private val homePageRepository: SectionRepository) {

    operator fun invoke(sectionName: String): Flow<Result<List<ArticleMeta>>> =
        homePageRepository.getPageBuilderSection(sectionName)
            .map { pageBuilderApiResponse ->
                pageBuilderApiResponse.map {
                    WearDataManager.getPageBuilderArticleMetaList(it.regionsContainer.mainRegion.items)
                }
            }

}