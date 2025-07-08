package com.opacitylabs.opacitycore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.opacitylabs.opacitycore.JsonToAnyConverter.Companion.parseJsonElementToAny
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.mozilla.geckoview.GeckoRuntime

object OpacityCore {
    enum class Environment {
        TEST,
        LOCAL,
        SANDBOX,
        STAGING,
        PRODUCTION,
    }

    private lateinit var appContext: Context
    private lateinit var cryptoManager: CryptoManager
    private lateinit var _url: String
    private var headers: Bundle = Bundle()
    private lateinit var sRuntime: GeckoRuntime

    init {
        System.loadLibrary("OpacityCore")
    }

    @JvmStatic
    fun initialize(
        apiKey: String,
        dryRun: Boolean,
        environment: Environment,
        showErrorsInWebView: Boolean
    ): Int {
        return init(apiKey, dryRun, environment.ordinal, showErrorsInWebView)
    }

    @JvmStatic
    fun setContext(context: Context) {
        appContext = context
        sRuntime = GeckoRuntime.create(appContext.applicationContext)
        cryptoManager = CryptoManager(appContext.applicationContext)
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
        headers.putString(key.lowercase(), value)
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

    fun getBrowserCookiesForCurrentUrl(): String {
        val cookiesIntent = Intent("com.opacitylabs.opacitycore.GET_COOKIES_FOR_CURRENT_URL")
        val resultReceiver = CookieResultReceiver()
        cookiesIntent.putExtra("receiver", resultReceiver)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(cookiesIntent)
        val json = resultReceiver.waitForResult(1000) // Wait up to 1 second for the result
        return json.toString()
    }

    fun getBrowserCookiesForDomain(domain: String): String {
        val cookiesIntent = Intent("com.opacitylabs.opacitycore.GET_COOKIES_FOR_DOMAIN")
        val resultsReceiver = CookieResultReceiver()
        cookiesIntent.putExtra("receiver", resultsReceiver)
        cookiesIntent.putExtra("domain", domain)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(cookiesIntent)
        val json = resultsReceiver.waitForResult(1000)
        return json.toString()
    }

    fun closeBrowser() {
        val closeIntent = Intent("com.opacitylabs.opacitycore.CLOSE_BROWSER")
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(closeIntent)
    }

    private fun parseOpacityError(error: String?): OpacityError {
        if (error == null) {
            return OpacityError("UnknownError", "No Message")
        }
        return try {
            val json = Json.parseToJsonElement(error).jsonObject
            val code = json["code"]?.toString()?.replace("\"", "") ?: "unknown"
            val description = json["description"]?.toString()?.replace("\"", "") ?: "unknown"
            OpacityError(code, description)
        } catch (e: Exception) {
            OpacityError("UnknownError", error)
        }
    }

    @JvmStatic
    suspend fun get(name: String, params: Map<String, Any?>?): Result<Map<String, Any?>> {
        return withContext(Dispatchers.IO) {
            val paramsString = params?.let { Json.encodeToString(it) }
            val res = getNative(name, paramsString)
            if (res.status != 0) {
                return@withContext Result.failure(parseOpacityError(res.err))
            }

            val map: Map<String, Any?> =
                Json.parseToJsonElement(res.data!!).jsonObject.mapValues {
                    parseJsonElementToAny(it.value)
                }

            return@withContext Result.success(map)
        }
    }

    private external fun init(
        apiKey: String,
        dryRun: Boolean,
        environment: Int,
        showErrorsInWebView: Boolean
    ): Int

    private external fun getNative(name: String, params: String?): OpacityResponse
    external fun getSdkVersions(): String
    external fun emitWebviewEvent(eventJson: String)
}
