// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.lifecycle

import com.wapo.kmpshared.core.config.KMPEnv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sustained app activation state (maps to iOS UIScene.ActivationState) — current value, repeats deduped. */
enum class AppActivationState(
    val label: String
) {
    Active("active"),
    Inactive("inactive"),
    Background("background")
}

enum class ReachabilityType(
    val label: String
) {
    Wifi("wifi"),
    Cell("cell"),
}

sealed interface ReachabilityStatus {
    data object Unknown : ReachabilityStatus

    data object Offline : ReachabilityStatus

    data class Reachable(
        val type: ReachabilityType,
        val isExpensive: Boolean,
        val isConstrained: Boolean,
    ) : ReachabilityStatus

    val isReachable: Boolean get() = this !is Offline
}

internal fun ReachabilityStatus.getNetworkType(): String =
    when (this) {
        is ReachabilityStatus.Reachable -> type.label
        else -> "unknown"
    }

/**
 * A lifecycle change reported by the platform shim via [AppLifecycleWriter.receiveChange].
 * Each case carries its payload so the read side stays whole state, not lossy edges.
 */
sealed interface AppLifecycleChange {
    data class Activation(
        val state: AppActivationState,
    ) : AppLifecycleChange

    data class Reachability(
        val status: ReachabilityStatus,
    ) : AppLifecycleChange

    /**
     * Login identity of the current account; `null` = signed out.
     * TODO: Account handling may change: possibly a dedicated and/or networking deriving account
     * from lifecycle.  The `id` is carried so consumers can tell whether their cached account data
     * still applies.
     */
    data class Account(
        val id: String?,
    ) : AppLifecycleChange
}

data class AppEnvironment(
    val env: KMPEnv,
    val appBuild: String?,
    val appVersion: String?,
    val device: String?,
    val supportID: String,
    val osVersion: String,
)

interface AppLifecycle {
    val env: AppEnvironment
    val activationState: StateFlow<AppActivationState>
    val reachability: StateFlow<ReachabilityStatus>
    val account: StateFlow<String?>
}

interface AppLifecycleWriter {
    fun receiveChange(change: AppLifecycleChange)
}

/**
 * Concrete app lifecycle instantiated platform-side: handed to the OS-notification shim as an
 * [AppLifecycleWriter] and injected into Koin as the read-only [AppLifecycle].
 */
class DefaultAppLifecycle(
    override val env: AppEnvironment,
) : AppLifecycle,
    AppLifecycleWriter {
    private val _activationState = MutableStateFlow(AppActivationState.Background)
    override val activationState: StateFlow<AppActivationState> = _activationState.asStateFlow()

    private val _reachability = MutableStateFlow<ReachabilityStatus>(ReachabilityStatus.Unknown)
    override val reachability: StateFlow<ReachabilityStatus> = _reachability.asStateFlow()

    private val _account = MutableStateFlow<String?>(null)
    override val account: StateFlow<String?> = _account.asStateFlow()

    /** Routes each change onto its StateFlow. */
    override fun receiveChange(change: AppLifecycleChange) {
        when (change) {
            is AppLifecycleChange.Activation -> _activationState.value = change.state
            is AppLifecycleChange.Reachability -> _reachability.value = change.status
            is AppLifecycleChange.Account -> _account.value = change.id
        }
    }
}
