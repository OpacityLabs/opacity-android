package com.opacitylabs.opacitycore

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent

object OpacityCore {
    private lateinit var appContext: Context
    private lateinit var cryptoManager: CryptoManager

    private lateinit var uri: Uri
    private lateinit var browserIntent: CustomTabsIntent
    private var headers: Bundle = Bundle()

    init {
        System.loadLibrary("OpacityCore")
    }

    fun initialize(context: Context, apiKey: String, dryRun: Boolean): Int {
        appContext = context
        cryptoManager = CryptoManager(appContext.applicationContext)
        return init(apiKey, dryRun)
    }

    fun securelySet(key: String, value: String) {
        cryptoManager.set(key, value)
    }

    fun securelyGet(key: String): String? {
        return cryptoManager.get(key)
    }

    fun prepareInAppBrowser(url: String) {
        uri = Uri.parse(url)

        val builder = CustomTabsIntent.Builder()
        browserIntent = builder.build()
    }

    fun setBrowserHeader(key: String, value: String) {
        headers.putString(key, value)
    }

    fun presentBrowser() {
        browserIntent.intent.putExtra(Browser.EXTRA_HEADERS, headers)

        try {
            browserIntent.launchUrl(appContext, uri)
        } catch (e: Exception) {
            Log.e("OpacityCore", "Failed to launch in-app browser", e)
        }
    }

    fun closeBrowser() {
        // Close the browser by sending an empty intent
        val intent = Intent()
        appContext.startActivity(intent)
    }

    external fun init(apiKey: String, dryRun: Boolean): Int
    external fun getUberRiderProfile()
}
