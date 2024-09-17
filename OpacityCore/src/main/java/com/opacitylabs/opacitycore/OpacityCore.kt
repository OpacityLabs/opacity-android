package com.opacitylabs.opacitycore

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection

//class NavigationCallback : CustomTabsCallback() {
//    override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
//        Log.d("CustomTabs", "Navigation event happened: " + navigationEvent)
//        when (navigationEvent) {
//            TAB_SHOWN -> Log.d("CustomTabs", "Tab shown")
//            TAB_HIDDEN -> Log.d("CustomTabs", "Tab hidden")
//            NAVIGATION_STARTED -> Log.d("CustomTabs", "Navigation started")
//            NAVIGATION_FINISHED -> Log.d("CustomTabs", "Navigation finished")
//            NAVIGATION_FAILED -> Log.d("CustomTabs", "Navigation failed")
//            NAVIGATION_ABORTED -> Log.d("CustomTabs", "Navigation aborted")
//        }
//
//        extras?.let {
//        Log.d("CustomTabs", "Extras found! 🟦 ${it.keySet()}, isEmpty?: ${it.isEmpty}")
//            for (key in it.keySet()) {
//                val value = it.get(key)
//                Log.d("CustomTabs","🟩 $key: $value")
//            }
//
//            val url = it.getString("android.intent.extra.URL")
//            url?.let { Log.d("CustomTabs", "URL: $url") }
//            val cookies = it.getStringArrayList("android.webkit.extra.HTTP_HEADERS")
//            cookies?.let { Log.d("CustomTabs", "Cookies: $cookies") }
//        }
//    }
//}

object OpacityCore {
    private lateinit var appContext: Context
    private lateinit var cryptoManager: CryptoManager

    private lateinit var uri: Uri
    private lateinit var _url: String
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
        _url = url
        uri = Uri.parse(url)

        val builder = CustomTabsIntent.Builder()
        builder.setSendToExternalDefaultHandlerEnabled(true)
//        builder.setSession()
        browserIntent = builder.build()
    }

    fun setBrowserHeader(key: String, value: String) {
        headers.putString(key, value)
    }

    fun presentBrowser() {
        val intent = Intent()
        intent.setClassName(appContext.packageName, "com.opacitylabs.opacitycore.InAppBrowserActivity")
        intent.putExtra("url", _url)
        appContext.startActivity(intent)

//        //        browserIntent.intent.putExtra(Browser.EXTRA_HEADERS, headers)
//
//        val session =
//                CustomTabsClient.bindCustomTabsService(
//                        appContext,
//                        CustomTabsClient.getPackageName(appContext, null),
//                        object : CustomTabsServiceConnection() {
//                            override fun onCustomTabsServiceConnected(
//                                    name: ComponentName,
//                                    client: CustomTabsClient
//                            ) {
//                                val session = client.newSession(NavigationCallback())
//                                session?.let {
//                                    // Attach session to the intent
//                                    val customTabsIntent = CustomTabsIntent.Builder(it).build()
//                                    customTabsIntent.intent.putExtra(Browser.EXTRA_HEADERS, headers)
//                                    customTabsIntent.launchUrl(appContext, uri)
//                                }
//                            }
//                            override fun onServiceDisconnected(name: ComponentName) {}
//                        }
//                )
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
