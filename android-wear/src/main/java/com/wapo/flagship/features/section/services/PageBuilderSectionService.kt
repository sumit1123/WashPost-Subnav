/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.services

import com.wapo.flagship.features.sections.model.PageBuilderAPIResponse
import com.wapo.flagship.network.APIResult
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API service interface to retrieve PageBuilder homepage.
 *
 */
interface PageBuilderSectionService {

    @GET("prod/v1/phone/")
    suspend fun getPageBuilderTopStories(): APIResult<PageBuilderAPIResponse>

    @GET("prod/v1/phone/{sectionName}")
    suspend fun getPageBuilderSection(
        @Path("sectionName") sectionName: String
    ): APIResult<PageBuilderAPIResponse>

}