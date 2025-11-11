package com.opacitylabs.opacitycore

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.opacitylabs.opacitycore.JsonConverter.Companion.mapToJsonElement
import com.opacitylabs.opacitycore.JsonConverter.Companion.parseJsonElementToAny
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.mozilla.geckoview.GeckoRuntime

object OpacityCore {
    enum class Environment(val code: Int) {
        LOCAL(1),
        SANDBOX(2),
        STAGING(3),
        PRODUCTION(4),
    }

    private lateinit var appContext: Context
    private lateinit var cryptoManager: CryptoManager
    private lateinit var _url: String
    private var headers: Bundle = Bundle()
    private lateinit var sRuntime: GeckoRuntime
    private var isBrowserActive = false

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
        return init(apiKey, dryRun, environment.code, showErrorsInWebView)
    }

    @JvmStatic
    fun setContext(context: Context) {
        appContext = context
        // Only create GeckoRuntime if it hasn't been created yet
        if (!::sRuntime.isInitialized) {
            sRuntime = GeckoRuntime.create(appContext.applicationContext)
        }
        cryptoManager = CryptoManager(appContext.applicationContext)
    }

    fun getRuntime(): GeckoRuntime {
        if (!::sRuntime.isInitialized) {
            throw IllegalStateException("GeckoRuntime is not initialized. Call initialize() first.")
        }
        return sRuntime
    }

    fun isAppForegrounded(): Boolean {
        return try {
            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningProcesses = activityManager.runningAppProcesses
            if (runningProcesses != null) {
                for (processInfo in runningProcesses) {
                    if (processInfo.processName == appContext.packageName) {
                        return processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER
    }

    fun getDeviceModel(): String {
        return Build.MODEL
    }

    fun getDeviceLocale(): String {
        return java.util.Locale.getDefault().toLanguageTag()
    }

    fun getScreenWidth(): Int {
        val displayMetrics = appContext.resources.displayMetrics
        return displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        val displayMetrics = appContext.resources.displayMetrics
        return displayMetrics.heightPixels
    }

    fun getScreenDensity(): Float {
        val displayMetrics = appContext.resources.displayMetrics
        return displayMetrics.density
    }

    fun getScreenDpi(): Int {
        val displayMetrics = appContext.resources.displayMetrics
        return displayMetrics.densityDpi
    }

    fun getDeviceCpu(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: Build.CPU_ABI
    }

    fun getDeviceCodename(): String {
        return Build.DEVICE
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

    fun presentBrowser(shouldIntercept: Boolean) {
        val intent = Intent(appContext, InAppBrowserActivity::class.java)
        intent.putExtra("url", _url)
        intent.putExtra("headers", headers)
        intent.putExtra("enableInterceptRequests", shouldIntercept)
        appContext.startActivity(intent)
        isBrowserActive = true
    }

    fun getBrowserCookiesForCurrentUrl(): String? {
        if (!isBrowserActive) {
            return null
        }

        val cookiesIntent = Intent("com.opacitylabs.opacitycore.GET_COOKIES_FOR_CURRENT_URL")
        val resultReceiver = CookieResultReceiver()
        cookiesIntent.putExtra("receiver", resultReceiver)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(cookiesIntent)
        val json = resultReceiver.waitForResult(1000) // Wait up to 1 second for the result
        return json?.toString()
    }

    fun getBrowserCookiesForDomain(domain: String): String? {
        if (!isBrowserActive) {
            return null
        }

        val cookiesIntent = Intent("com.opacitylabs.opacitycore.GET_COOKIES_FOR_DOMAIN")
        val resultsReceiver = CookieResultReceiver()
        cookiesIntent.putExtra("receiver", resultsReceiver)
        cookiesIntent.putExtra("domain", domain)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(cookiesIntent)
        val json = resultsReceiver.waitForResult(1000)
        return json?.toString()
    }

    fun closeBrowser() {
        val closeIntent = Intent("com.opacitylabs.opacitycore.CLOSE_BROWSER")
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(closeIntent)
    }

    fun changeUrlInBrowser(url: String) {
        val changeUrlIntent = Intent("com.opacitylabs.opacitycore.CHANGE_URL")
        changeUrlIntent.putExtra("url", url)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(changeUrlIntent)
    }

    fun onBrowserDestroyed() {
        isBrowserActive = false
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
            val paramsString = params?.let {
                val jsonElement = mapToJsonElement(it)
                Json.encodeToString(jsonElement)
            }

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
