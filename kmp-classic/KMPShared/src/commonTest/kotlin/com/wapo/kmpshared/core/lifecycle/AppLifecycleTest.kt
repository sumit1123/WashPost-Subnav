// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.lifecycle

import com.wapo.kmpshared.core.config.KMPEnv
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLifecycleTest {
    private val testAppEnvironment =
        AppEnvironment(
            env = KMPEnv.Test,
            appBuild = null,
            appVersion = null,
            device = null,
            supportID = "0000-0000-0000",
            osVersion = "",
        )

    @Test
    fun activationState_startsBackgroundThenTracksTransitions() {
        val lifecycle = DefaultAppLifecycle(testAppEnvironment)

        assertEquals(AppActivationState.Background, lifecycle.activationState.value)

        lifecycle.receiveChange(AppLifecycleChange.Activation(AppActivationState.Active))
        assertEquals(AppActivationState.Active, lifecycle.activationState.value)

        lifecycle.receiveChange(AppLifecycleChange.Activation(AppActivationState.Inactive))
        assertEquals(AppActivationState.Inactive, lifecycle.activationState.value)

        lifecycle.receiveChange(AppLifecycleChange.Activation(AppActivationState.Background))
        assertEquals(AppActivationState.Background, lifecycle.activationState.value)
    }

    @Test
    fun reachability_startsUnknownThenTracksTransitions() {
        val lifecycle = DefaultAppLifecycle(testAppEnvironment)

        // Unknown counts as reachable at startup, matching iOS.
        assertEquals(ReachabilityStatus.Unknown, lifecycle.reachability.value)
        assertEquals(true, lifecycle.reachability.value.isReachable)

        val wifi = ReachabilityStatus.Reachable(ReachabilityType.Wifi, isExpensive = false, isConstrained = false)
        lifecycle.receiveChange(AppLifecycleChange.Reachability(wifi))
        assertEquals(wifi, lifecycle.reachability.value)

        // "Lost" is just a transition to Offline — no separate event needed.
        lifecycle.receiveChange(AppLifecycleChange.Reachability(ReachabilityStatus.Offline))
        assertEquals(ReachabilityStatus.Offline, lifecycle.reachability.value)
        assertEquals(false, lifecycle.reachability.value.isReachable)
    }

    @Test
    fun account_startsSignedOutThenTracksIdentity() {
        val lifecycle = DefaultAppLifecycle(testAppEnvironment)

        assertEquals(null, lifecycle.account.value)

        lifecycle.receiveChange(AppLifecycleChange.Account("user-123"))
        assertEquals("user-123", lifecycle.account.value)

        // Switched account is observable as an identity change.
        lifecycle.receiveChange(AppLifecycleChange.Account("user-456"))
        assertEquals("user-456", lifecycle.account.value)

        // Signed out.
        lifecycle.receiveChange(AppLifecycleChange.Account(null))
        assertEquals(null, lifecycle.account.value)
    }
}
