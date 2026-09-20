// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.di

import com.wapo.kmpshared.features.conversations.presentation.CommentsStore
import com.wapo.kmpshared.features.feedback.domain.FeedbackRepository
import com.wapo.kmpshared.logger.WPLogger
import com.wapo.kmpshared.util.KMPURL
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class AppDependencies {
    private object KoinBridge : KoinComponent {
        fun logger(): WPLogger? = runCatching { get<WPLogger>() }.getOrNull()

        fun feedback(): FeedbackRepository? = runCatching { get<FeedbackRepository>() }.getOrNull()

        fun createStore(
            storyURL: KMPURL,
            arcID: String?,
        ): CommentsStore? =
            runCatching { get<CommentsStore> { parametersOf(storyURL, arcID) } }
                .getOrNull()
    }

    val logger: WPLogger? get() = KoinBridge.logger()

    val feedback: FeedbackRepository? get() = KoinBridge.feedback()

    fun makeCommentsStore(
        storyURL: KMPURL,
        arcID: String?,
    ): CommentsStore? = KoinBridge.createStore(storyURL, arcID)
}
