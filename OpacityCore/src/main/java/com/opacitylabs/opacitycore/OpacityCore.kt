package com.opacitylabs.opacitycore

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle


object OpacityCore {
    private lateinit var appContext: Context
    private lateinit var cryptoManager: CryptoManager

    private lateinit var uri: Uri
    private lateinit var _url: String
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
        _url = url
        uri = Uri.parse(url)

//        val builder = CustomTabsIntent.Builder()
//        builder.setSendToExternalDefaultHandlerEnabled(true)
//        builder.setSession()
//        browserIntent = builder.build()
    }

    fun setBrowserHeader(key: String, value: String) {
        headers.putString(key, value)
    }

    fun presentBrowser() {
        val intent = Intent()
        intent.setClassName(appContext.packageName, "com.opacitylabs.opacitycore.InAppBrowserActivity")
        intent.putExtra("url", _url)
        intent.putExtra("headers", headers)
        appContext.startActivity(intent)
    }

    fun closeBrowser() {
        // Close the browser by sending an empty intent
        val intent = Intent()
        appContext.startActivity(intent)
    }

    fun sampleRedirection() {
        val flow =
                "{\n" +
                        "          \"version\": \"1.0.0\",\n" +
                        "          \"name\": \"authWebDriver\",\n" +
                        "          \"context_generator\": {},\n" +
                        "          \"steps\": [\n" +
                        "            {\n" +
                        "            \"name\": \"webview\",\n" +
                        "            \"url\": \"http://localhost:8666/uber_redirect\",\n" +
                        "            \"is_browser_step\": true,\n" +
                        "            \"await_events\": [\n" +
                        "              {\n" +
                        "              \"event\": \"navigation\",\n" +
                        "              \"base_url_ios\": \"uberlogin://auth3.uber.com/applogin\"\n" +
                        "              }\n" +
                        "            ]\n" +
                        "            }\n" +
                        "          ]\n" +
                        "          }"
        executeFlow(flow)
    }

    external fun init(apiKey: String, dryRun: Boolean): Int
    external fun getUberRiderProfile()
    external fun executeFlow(flow: String)
}
