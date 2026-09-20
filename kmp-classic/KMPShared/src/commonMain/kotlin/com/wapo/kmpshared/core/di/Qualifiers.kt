// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.di

/**
 * Centralized qualifiers for Koin dependency injection.
 * Use these constants with @Named or qualifier() to avoid string hardcoding.
 */
object Qualifiers {
    /**
     * Used with @Named to identify a long-lived CoroutineScope for background tasks.
     * Use this for "fire and forget" operations that should outlive the caller's lifecycle.
     */
    const val APPLICATION_SCOPE = "APPLICATION_SCOPE"

    const val FEEDBACK = "Feedback"

    const val CONVERSATIONS = "Conversations"

    const val LOGGER_PACKAGE = "com.wapo.kmpshared.logger"

    /**
     * The package root for the Feedback feature.
     * Used in @ComponentScan to help Koin find @Single and @Factory classes.
     */
    const val FEEDBACK_PACKAGE = "com.wapo.kmpshared.features.feedback"

    /**
     * The package root for the Native Convos feature
     */

    const val CONVERSATIONS_PACKAGE = "com.wapo.kmpshared.features.conversations"
}
