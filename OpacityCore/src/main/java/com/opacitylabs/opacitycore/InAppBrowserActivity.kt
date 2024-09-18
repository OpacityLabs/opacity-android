package com.opacitylabs.opacitycore

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class InAppBrowserActivity : AppCompatActivity() {

    private val closeReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == "com.opacitylabs.opacitycore.CLOSE_BROWSER") {
                        finish()
                    }
                }
            }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(
                closeReceiver,
                IntentFilter("com.opacitylabs.opacitycore.CLOSE_BROWSER")
        )

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val closeButton = Button(this)
        closeButton.text = "Close"
        closeButton.setOnClickListener {
            val json =
                    "{\"event\": \"close\", \"id\": \"${System.currentTimeMillis().toString()}\"}"
            OpacityCore.emitWebviewEvent(json)
        }

        val layoutParams =
                ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.START or Gravity.CENTER_VERTICAL
                )
        supportActionBar?.setCustomView(closeButton, layoutParams)
        supportActionBar?.setDisplayShowCustomEnabled(true)

        val webView = WebView(this)
        setContentView(webView)

        val url = intent.getStringExtra("url") ?: return
        val headers = intent.getBundleExtra("headers")

        webView.settings.javaScriptEnabled = true

        val webViewClient =
                object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                    ): Boolean {
                        val newUrl = request.url.toString()
                        handleNavigation(newUrl)
                        return false
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        handleNavigation(url)
                    }
                }

        webView.webViewClient = webViewClient

        val headersMap =
                (headers?.keySet()?.associateWith { headers.getString(it) } ?: emptyMap())
                        .toMutableMap()
        // important to remove to prevent android webview detection on services
        headersMap["X-Requested-With"] = ""

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true) // For devices below API level 21
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.loadUrl(url, headersMap)
    }

    private fun convertToJsonString(map: Map<String, String>): String {
        val jsonString = StringBuilder()
        jsonString.append("{")
        map.forEach { (key, value) -> jsonString.append("\"$key\": \"$value\", ") }
        if (jsonString.length > 1) {
            jsonString.delete(jsonString.length - 2, jsonString.length)
        }
        jsonString.append("}")
        return jsonString.toString()
    }
    private fun handleNavigation(url: String) {
        CookieManager.getInstance().flush()
        val cookies = extractCookies(url)
        val json =
                "{\"event\": \"navigation\"," +
                        "\"url\": \"$url\", \"cookies\": ${convertToJsonString(cookies)}, \"id\": \"${
                    System.currentTimeMillis().toString()
                }\"}"

        OpacityCore.emitWebviewEvent(json)
    }

    private fun extractDomainFromUrl(url: String): String {
        val uri = Uri.parse(url)
        return uri.host ?: ""
    }

    private fun extractCookies(url: String): Map<String, String> {
        val cookieManager = CookieManager.getInstance()
        val domain = extractDomainFromUrl(url)
        val cookies = cookieManager.getCookie(domain)
        val cookieMap = mutableMapOf<String, String>()
        if (!cookies.isNullOrEmpty()) {
            val cookiePairs = cookies.split(";")
            for (cookiePair in cookiePairs) {
                val pair = cookiePair.trim().split("=")
                if (pair.size == 2) {
                    val key = pair[0].trim()
                    val value = pair[1].trim()
                    cookieMap[key] = value
                }
            }
        }
        return cookieMap
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver)
    }
}
