package com.wapo.flagship.kmp.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.wapo.kmpshared.core.lifecycle.AppActivationState
import com.wapo.kmpshared.core.lifecycle.AppLifecycleChange
import com.wapo.kmpshared.core.lifecycle.AppLifecycleWriter
import com.wapo.kmpshared.core.lifecycle.ReachabilityStatus
import com.wapo.kmpshared.core.lifecycle.ReachabilityType
import com.washingtonpost.android.paywall.PaywallService

/** Bridges Android process, connectivity, and account state into KMP. */
class KMPLifecycleObserver(
    context: Context,
    private val writer: AppLifecycleWriter,
) : DefaultLifecycleObserver {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var lastAccountID: String? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = publishReachability()

        override fun onLost(network: Network) = publishReachability()

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            publishReachability(networkCapabilities)
        }
    }

    init {
        publishInitialState()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    fun close() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        writer.receiveChange(AppLifecycleChange.Activation(AppActivationState.Inactive))
    }

    override fun onResume(owner: LifecycleOwner) {
        writer.receiveChange(AppLifecycleChange.Activation(AppActivationState.Active))
        publishAccount()
    }

    override fun onPause(owner: LifecycleOwner) {
        writer.receiveChange(AppLifecycleChange.Activation(AppActivationState.Inactive))
        publishAccount()
    }

    override fun onStop(owner: LifecycleOwner) {
        writer.receiveChange(AppLifecycleChange.Activation(AppActivationState.Background))
    }

    private fun publishInitialState() {
        val state = when {
            lifecycleState.isAtLeast(Lifecycle.State.RESUMED) -> AppActivationState.Active
            lifecycleState.isAtLeast(Lifecycle.State.STARTED) -> AppActivationState.Inactive
            else -> AppActivationState.Background
        }
        writer.receiveChange(AppLifecycleChange.Activation(state))
        publishReachability()
        publishAccount()
    }

    private fun publishReachability(capabilities: NetworkCapabilities? = currentCapabilities()) {
        val status = if (capabilities == null) {
            ReachabilityStatus.Offline
        } else {
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ReachabilityType.Wifi
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ReachabilityType.Cell
                else -> ReachabilityType.Cell
            }
            ReachabilityStatus.Reachable(
                type = type,
                isExpensive = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                isConstrained = false,
            )
        }
        writer.receiveChange(AppLifecycleChange.Reachability(status))
    }

    private fun currentCapabilities(): NetworkCapabilities? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val network = connectivityManager.activeNetwork ?: return null
        return connectivityManager.getNetworkCapabilities(network)
            ?.takeIf { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
    }

    private fun publishAccount() {
        val accountID = PaywallService.getInstance()?.loggedInUser?.uuid
        if (accountID != lastAccountID) {
            lastAccountID = accountID
            writer.receiveChange(AppLifecycleChange.Account(accountID))
        }
    }

    private val lifecycleState: Lifecycle.State
        get() = ProcessLifecycleOwner.get().lifecycle.currentState
}
