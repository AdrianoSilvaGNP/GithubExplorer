package com.adrianosilva.githubexplorer.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import timber.log.Timber

class AndroidConnectivityManager(context: Context) {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager

    var hasInternetConnection: Boolean = false
        private set

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val networkCallback = object: ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val internet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                hasInternetConnection = internet
                Timber.d("Network onCapabilitiesChanged: internet=$internet")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                hasInternetConnection = false
                Timber.d("Connection Lost")
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
}