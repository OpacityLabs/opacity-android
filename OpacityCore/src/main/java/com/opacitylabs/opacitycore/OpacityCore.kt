package com.opacitylabs.opacitycore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime

object OpacityCore {

    private lateinit var appContext: Context
    private lateinit var cryptoManager: CryptoManager
    private lateinit var _url: String
    private var headers: Bundle = Bundle()
    private lateinit var sRuntime: GeckoRuntime

    init {
        System.loadLibrary("OpacityCore")
    }

    fun initialize(context: Context, apiKey: String, dryRun: Boolean): Int {
        appContext = context
        sRuntime = GeckoRuntime.create(context.applicationContext)
        cryptoManager = CryptoManager(appContext.applicationContext)
        return init(apiKey, dryRun)
    }

    fun getRuntime(): GeckoRuntime {
        if (!::sRuntime.isInitialized) {
            throw IllegalStateException("GeckoRuntime is not initialized. Call initialize() first.")
        }
        return sRuntime
    }

    fun securelySet(key: String, value: String) {
        cryptoManager.set(key, value)
    }

    fun securelyGet(key: String): String? {
        return cryptoManager.get(key)
    }

    fun prepareInAppBrowser(url: String) {
        headers = Bundle()
        _url = url
    }

    fun setBrowserHeader(key: String, value: String) {
        headers.putString(key, value)
    }

    fun presentBrowser() {
        val intent = Intent()
        intent.setClassName(
            appContext.packageName,
            "com.opacitylabs.opacitycore.InAppBrowserActivity"
        )
        intent.putExtra("url", _url)
        intent.putExtra("headers", headers)
        appContext.startActivity(intent)
    }

    fun closeBrowser() {
        val closeIntent = Intent("com.opacitylabs.opacitycore.CLOSE_BROWSER")
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(closeIntent)
        Log.d("OpacityCore", "Intent dispatched")
    }

    suspend fun getUberRiderProfile(): OpacityResponse {
        return withContext(Dispatchers.IO) { getUberRiderProfileNative() }
    }

    suspend fun getUberRiderTripHistory(cursor: String): OpacityResponse {
        return withContext(Dispatchers.IO) { getUberRiderTripHistoryNative(cursor) }
    }

    suspend fun getUberRiderTrip(id: String): OpacityResponse {
        return withContext(Dispatchers.IO) { getUberRiderTripNative(id) }
    }

    suspend fun getUberDriverProfile(): OpacityResponse {
        return withContext(Dispatchers.IO) { getUberDriverProfileNative() }
    }

    suspend fun getUberDriverTrips(
        startDate: String,
        endDate: String,
        cursor: String
    ): OpacityResponse {
        return withContext(Dispatchers.IO) { getUberDriverTripsNative(startDate, endDate, cursor) }
    }

    suspend fun getRedditAccount(): OpacityResponse {
        return withContext(Dispatchers.IO) { getRedditAccountNative() }
    }

    suspend fun getRedditFollowedSubreddits(): OpacityResponse {
        return withContext(Dispatchers.IO) { getRedditFollowedSubredditsNative() }
    }

    suspend fun getRedditComments(): OpacityResponse {
        return withContext(Dispatchers.IO) { getRedditCommentsNative() }
    }

    suspend fun getRedditPosts(): OpacityResponse {
        return withContext(Dispatchers.IO) { getRedditPostsNative() }
    }

    suspend fun getZabkaAccount(): OpacityResponse {
        return withContext(Dispatchers.IO) { getZabkaAccountNative() }
    }

    suspend fun getZabkaPoints(): OpacityResponse {
        return withContext(Dispatchers.IO) { getZabkaPointsNative() }
    }

    private external fun init(apiKey: String, dryRun: Boolean): Int
    private external fun executeFlow(flow: String)
    external fun emitWebviewEvent(eventJson: String)
    private external fun getUberFareEstimate(
        pickupLatitude: Double,
        pickupLongitude: Double,
        destinationLatitude: Double,
        destinationLongitude: Double
    ): OpacityResponse

    private external fun getUberRiderProfileNative(): OpacityResponse
    private external fun getUberRiderTripHistoryNative(cursor: String): OpacityResponse
    private external fun getUberRiderTripNative(id: String): OpacityResponse
    private external fun getUberDriverProfileNative(): OpacityResponse
    private external fun getUberDriverTripsNative(
        startDate: String,
        endDate: String,
        cursor: String
    ): OpacityResponse

    private external fun getRedditAccountNative(): OpacityResponse
    private external fun getRedditFollowedSubredditsNative(): OpacityResponse
    private external fun getRedditCommentsNative(): OpacityResponse
    private external fun getRedditPostsNative(): OpacityResponse
    private external fun getZabkaAccountNative(): OpacityResponse
    private external fun getZabkaPointsNative(): OpacityResponse
}
