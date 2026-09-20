// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.features.feedback.data

import com.wapo.kmpshared.core.config.FeedbackServiceConfig
import com.wapo.kmpshared.core.di.Qualifiers
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.core.network.BaseService
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.features.feedback.data.remote.FeedbackRequestDto
import com.wapo.kmpshared.features.feedback.domain.model.FeedbackSurface
import com.wapo.kmpshared.logger.WPLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class RemoteFeedbackService(
    @Named(Qualifiers.APPLICATION_SCOPE) private val externalScope: CoroutineScope,
    @Named(Qualifiers.FEEDBACK) private val networkClient: HttpClient,
    private val coder: Json,
    private val config: FeedbackServiceConfig,
    appLifecycle: AppLifecycle,
    platformNetworkProvider: PlatformNetworkProvider,
    logger: WPLogger,
) : BaseService(appLifecycle, platformNetworkProvider, externalScope, logger),
    FeedbackService {
    override suspend fun submitFeedback(
        dto: FeedbackRequestDto,
        surface: FeedbackSurface,
    ) = toKMPResult("SubmitFeedback") {
        val baseURL = config.baseURL
        networkClient
            .post(baseURL.toString()) {
                setBody(coder.encodeToString(dto))
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "*/*")
                header("surface", surface.name)
            }.body<Unit>()
    }
}
