package com.wapo.flagship.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.CountDownTimer
import androidx.annotation.NonNull
import androidx.core.net.ConnectivityManagerCompat

class ConnectivityMonitor private constructor(
    @NonNull val context: Context,
) {
    private val connectivityManager: ConnectivityManager = (
        context.getApplicationContext().getSystemService(
            Context.CONNECTIVITY_SERVICE,
        ) as ConnectivityManager
    )
    private var isPoorNetwork = false
    private var isNetworkCallbackRegistered = false
    private var networkListener: NetworkListener? = null
    private var countDownTimer: CountDownTimer? = null

    enum class ConnectivityState {
        UNMETERED,
        METERED,
        METERED_AND_UNRESTRICTED,
        METERED_AND_ALLOWED,
        METERED_AND_RESTRICTED,
    }

    interface NetworkListener {
        fun isCheckNetworkEnable(enable: () -> Unit)

        fun onPoorNetwork()

        fun onNetworkOk()
    }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            // network is available for use
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
            }

            // Network capabilities have changed for the network
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                // Check if the network is wifi
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // Check the Android version > than api 29 to use networkCapabilities.signalStrength
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (networkCapabilities.signalStrength < SIGNAL_STRENGTH_dBm_VALUE) {
                            handlePoorNetworkConnection()
                        } else {
                            handleGoodNetworkConnection()
                        }
                    } else {
                        // Use wifiManager for Android api < 29
                        val wifiManager =
                            context.applicationContext.getSystemService(
                                Context.WIFI_SERVICE,
                            ) as WifiManager
                        val level =
                            WifiManager.calculateSignalLevel(
                                wifiManager.connectionInfo.rssi,
                                SIGNAL_LEVEL_SCALE_VALUE,
                            )

                        if (level < SIGNAL_STRENGTH_VALUE) {
                            handlePoorNetworkConnection()
                        } else {
                            handleGoodNetworkConnection()
                        }
                    }
                } else {
                    // Check linkUpstreamBandwidthKbps and linkDownstreamBandwidthKbps for cellular connections
                    if (networkCapabilities.linkUpstreamBandwidthKbps <= LOW_UPSTREAM_KBPS_VALUE ||
                        networkCapabilities.linkDownstreamBandwidthKbps <= LOW_DOWNSTREAM_KBPS_VALUE
                    ) {
                        handlePoorNetworkConnection()
                    } else {
                        handleGoodNetworkConnection()
                    }
                }
            }
        }

    val networkRequest =
        NetworkRequest
            .Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

    fun restrictBackgroundStatus(): ConnectivityState {
        var state: ConnectivityState
        connectivityManager.apply {
            // Checks if the device is on a metered network
            if (ConnectivityManagerCompat.isActiveNetworkMetered(this)) {
                state = ConnectivityState.METERED
                // Checks user’s Data Saver settings.
                when (ConnectivityManagerCompat.getRestrictBackgroundStatus(this)) {
                    ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_DISABLED -> {
                        // Data Saver is disabled. Since the device is connected to a
                        // metered network, the app should use less data wherever possible.
                        state = ConnectivityState.METERED_AND_UNRESTRICTED
                    }
                    ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> {
                        // The app is allowed to bypass Data Saver. Nevertheless, wherever possible,
                        // the app should use less data in the foreground and background.
                        state = ConnectivityState.METERED_AND_ALLOWED
                    }
                    ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_ENABLED -> {
                        // Background data usage is blocked for this app. Wherever possible,
                        // the app should also use less data in the foreground.
                        state = ConnectivityState.METERED_AND_RESTRICTED
                    }
                }
            } else {
                // The device is not on a metered network.
                // Use data as required to perform syncs, downloads, and updates.
                state = ConnectivityState.UNMETERED
            }
        }
        return state
    }

    fun isOnMeteredNetwork(): Boolean {
        val connectivityState = restrictBackgroundStatus()
        return (connectivityState == ConnectivityState.METERED) or
            (connectivityState == ConnectivityState.METERED_AND_UNRESTRICTED) or
            (connectivityState == ConnectivityState.METERED_AND_ALLOWED) or
            (connectivityState == ConnectivityState.METERED_AND_RESTRICTED)
    }

    fun hasDeviceLevelDataRestriction(): Boolean = restrictBackgroundStatus() == ConnectivityState.METERED_AND_RESTRICTED

    fun isDataUsageRestricted(): Boolean = isOnMeteredNetwork() || hasDeviceLevelDataRestriction()

    fun registerToNetworkChanges(listener: NetworkListener) {
        networkListener = listener
        connectivityManager.requestNetwork(networkRequest, networkCallback)
        isNetworkCallbackRegistered = true
    }

    fun unRegisterToNetworkChanges() {
        networkListener = null
        isPoorNetwork = false
        countDownTimer?.cancel()
        countDownTimer = null
        if (isNetworkCallbackRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isNetworkCallbackRegistered = false
        }
    }

    private fun handlePoorNetworkConnection() {
        networkListener?.isCheckNetworkEnable {
            isPoorNetwork = true
            if (countDownTimer == null) {
                createNetworkJobListener()
            }
        }
    }

    private fun handleGoodNetworkConnection() {
        isPoorNetwork = false
        countDownTimer?.cancel()
        countDownTimer = null
        networkListener?.onNetworkOk()
    }

    private fun createNetworkJobListener() {
        countDownTimer =
            object : CountDownTimer(
                CONTINUE_LOW_CONNECTIVITY_TIMER,
                CONTINUE_LOW_CONNECTIVITY_TIMER,
            ) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    if (isPoorNetwork) {
                        networkListener?.onPoorNetwork()
                    }
                    countDownTimer = null
                }
            }.also {
                it.start()
            }
    }

    companion object {
        private const val TAG = "TWPConnectivityMonitor"
        private const val LOW_DOWNSTREAM_KBPS_VALUE = 4300
        private const val LOW_UPSTREAM_KBPS_VALUE = 1800
        private const val SIGNAL_STRENGTH_VALUE = 2
        private const val SIGNAL_LEVEL_SCALE_VALUE = 10
        private const val SIGNAL_STRENGTH_dBm_VALUE = -67
        private const val CONTINUE_LOW_CONNECTIVITY_TIMER = 15000L

        @Volatile
        private var instance: ConnectivityMonitor? = null

        @JvmStatic
        fun getInstance(
            @NonNull context: Context,
        ): ConnectivityMonitor =
            instance ?: synchronized(this) {
                instance ?: ConnectivityMonitor(context).also {
                    instance = it
                }
            }
    }
}
