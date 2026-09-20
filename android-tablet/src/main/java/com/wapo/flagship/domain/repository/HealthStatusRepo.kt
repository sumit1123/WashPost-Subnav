// Copyright (c) 2026 The Washington Post. All rights reserved.
package com.wapo.flagship.domain.repository

import com.wapo.flagship.features.backendhealth.models.BackendHealthStatus

interface HealthStatusRepo {

    suspend fun fetchHealthStatus(
        healthMonitorURL: String?,
        fallbackURL: String?,
        fallbackStaticURL: String?
    ): BackendHealthStatus

    suspend fun isFailoverActive(): Boolean

    suspend fun setFailoverActive(value: Boolean)
}
