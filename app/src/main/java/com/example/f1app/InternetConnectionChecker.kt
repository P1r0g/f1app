package com.example.f1app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class InternetConnectionChecker(context: Context) {
    private val connectivityManager = context.applicationContext
        .getSystemService(ConnectivityManager::class.java)

    fun isAvailable(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        // Wi-Fi being enabled does not guarantee access to the internet.
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
