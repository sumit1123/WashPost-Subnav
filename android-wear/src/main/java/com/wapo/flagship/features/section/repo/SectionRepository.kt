/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.repo

import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.features.sections.model.PageBuilderAPIResponse
import kotlinx.coroutines.flow.Flow

interface SectionRepository {

    fun getFusionSection(sectionName: String): Flow<Result<GridEntity>>
    fun getPageBuilderSection(sectionName: String): Flow<Result<PageBuilderAPIResponse>>

}