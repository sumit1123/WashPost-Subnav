/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.services

import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.network.APIResult
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API service interface to retrieve Fusion homepage.
 *
 */
interface FusionSectionService {

    @GET("fusion_prod/v2/")
    suspend fun getFusionTopStories(): APIResult<GridEntity>

    @GET("fusion_prod/v2/{sectionName}")
    suspend fun getFusionSection(
        @Path("sectionName") sectionName: String
    ): APIResult<GridEntity>

}