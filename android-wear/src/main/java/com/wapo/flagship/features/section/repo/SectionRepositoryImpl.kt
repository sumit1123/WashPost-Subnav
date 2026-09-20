/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.repo

import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.features.section.datasource.remote.SectionRemoteDataSource
import com.wapo.flagship.features.sections.model.PageBuilderAPIResponse
import kotlinx.coroutines.flow.Flow


class SectionRepositoryImpl(
    private val homePageRemoteDataSource: SectionRemoteDataSource
) : SectionRepository {

    override fun getFusionSection(sectionName: String): Flow<Result<GridEntity>> =
        homePageRemoteDataSource.getFusionSection(sectionName)

    override fun getPageBuilderSection(sectionName: String): Flow<Result<PageBuilderAPIResponse>> =
        homePageRemoteDataSource.getPageBuilderSection(sectionName)

}