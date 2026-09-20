/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.section.datasource.remote

import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.features.sections.model.PageBuilderAPIResponse
import com.wapo.flagship.features.section.services.FusionSectionService
import com.wapo.flagship.features.section.services.PageBuilderSectionService
import com.wapo.flagship.network.APIResult
import com.wapo.flagship.utils.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 *  Builds a flow of [GridEntity] or [PageBuilderAPIResponse] result data
 *  out of [APIResult] from [Articles2Service].
 *
 */
class SectionRemoteDataSourceImpl(
    private val fusionSectionService: FusionSectionService,
    private val pageBuilderSectionService: PageBuilderSectionService,
    private val dispatcherProvider: DispatcherProvider
) : SectionRemoteDataSource {

    override fun getFusionSection(sectionName: String): Flow<Result<GridEntity>> = flow {
        val apiResult =
            if (sectionName == "top stories")
                fusionSectionService.getFusionTopStories()
            else
                fusionSectionService.getFusionSection(sectionName)
        when (apiResult) {
            is APIResult.Success -> {
                emit(Result.success(apiResult.data!!))
            }
            is APIResult.Failure ->
                emit(Result.failure<GridEntity>(Exception(apiResult.rawResponse)))
            is APIResult.NetworkError ->
                emit(Result.failure<GridEntity>(apiResult.error))
        }
    }
        .catch { t ->
            emit(Result.failure(t))
        }
        .flowOn(dispatcherProvider.io)

    override fun getPageBuilderSection(sectionName: String): Flow<Result<PageBuilderAPIResponse>> =
        flow {
            val apiResult =
                if (sectionName == "top stories")
                    pageBuilderSectionService.getPageBuilderTopStories()
                else
                    pageBuilderSectionService.getPageBuilderSection(sectionName)
            when (apiResult) {
                is APIResult.Success ->
                    emit(Result.success(apiResult.data!!))
                is APIResult.Failure ->
                    emit(Result.failure<PageBuilderAPIResponse>(Exception(apiResult.rawResponse)))
                is APIResult.NetworkError ->
                    emit(Result.failure<PageBuilderAPIResponse>(apiResult.error))
            }
        }
            .catch { t ->
                emit(Result.failure(t))
            }
            .flowOn(dispatcherProvider.io)

}