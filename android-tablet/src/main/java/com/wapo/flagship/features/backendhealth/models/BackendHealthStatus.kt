// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.backendhealth.models

data class BackendHealthStatus(
    val isHealthy: Boolean,
    val fallbackURL: String? = null,
    val fallbackStaticURL: String? = null,
    val isLoading: Boolean = false,
)
