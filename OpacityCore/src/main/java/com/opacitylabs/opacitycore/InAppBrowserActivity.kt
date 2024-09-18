package com.opacitylabs.opacitycore

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class InAppBrowserActivity : AppCompatActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
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
        localBroadcastManager.registerReceiver(closeReceiver, IntentFilter("com.opacitylabs.opacitycore.CLOSE_BROWSER"))

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
                    //            override fun shouldInterceptRequest(
                    //                view: WebView?,
                    //                request: WebResourceRequest?
                    //            ): WebResourceResponse? {
                    //                if (request == null) {
                    //                    return null
                    //                }
                    //                //                         You can modify request headers here
                    //                val originalHeaders = request.requestHeaders ?: emptyMap()
                    //
                    //                // Remove or modify headers
                    //                val headers = HashMap(originalHeaders)
                    //
                    //                val newRequest =
                    //                    object : WebResourceRequest {
                    //                        override fun getUrl(): Uri = request.url
                    //                        override fun isForMainFrame(): Boolean =
                    // request.isForMainFrame
                    //                        override fun hasGesture(): Boolean =
                    // request.hasGesture()
                    //                        override fun isRedirect(): Boolean =
                    // request.isRedirect
                    //                        override fun getMethod(): String = request.method
                    //                        override fun getRequestHeaders(): Map<String, String>
                    // = headers
                    //                    }
                    //
                    //                // Proceed with the modified request
                    //                return super.shouldInterceptRequest(view, newRequest)
                    //            }

                    override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                    ): Boolean {
                        val newUrl = request.url.toString()
                        handleUrlRedirection(newUrl)
                        return false // Let the WebView handle the request
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        extractCookies(url)
                    }
                }

        webView.webViewClient = webViewClient

        val headersMap =
                (headers?.keySet()?.associateWith { headers.getString(it) } ?: emptyMap())
                        .toMutableMap()
        // important to remove to prevent android webview detection on services
        headersMap["X-Requested-With"] = ""

        webView.loadUrl(url, headersMap)
    }

    private fun handleUrlRedirection(newUrl: String) {
        // Handle redirection event, extract the URL
        Toast.makeText(this, "Redirected to: $newUrl", Toast.LENGTH_SHORT).show()
    }

    private fun extractCookies(url: String) {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)
        if (cookies != null) {
            // Do something with cookies, such as logging or processing
            Toast.makeText(this, "Cookies: $cookies", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver)
    }
}
