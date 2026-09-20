/*
 * Copyright (c) 2020. The Washington Post. All rights reserved.
 */
package com.wapo.flagship

/**
 * @author Jayesh Elamgodil 02/05/2020
 */
interface PaywallVerifyCallsSuppressHelper {
    fun isActivityLoadingPushAlert(): Boolean

    fun shouldSuppressPostPaywallInitVerifyCalls(): Boolean
}
