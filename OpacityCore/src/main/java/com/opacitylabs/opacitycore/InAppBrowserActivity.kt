package com.opacitylabs.opacitycore

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class InAppBrowserActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        val url = intent.getStringExtra("url") ?: return
        val headers = intent.getBundleExtra("headers")

        webView.settings.javaScriptEnabled = true

        webView.webViewClient =
                object : WebViewClient() {
                    override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                    ): WebResourceResponse? {
                        if (request == null) {
                            return null
                        }
                        //                         You can modify request headers here
                        val originalHeaders = request.requestHeaders ?: emptyMap()

                        // Remove or modify headers
                        val headers = HashMap(originalHeaders)
                        headers.remove("X-Requested-With")
//                        headers.remove("sec-ch-ua")
//                        headers.remove("sec-ch-ua-mobile")
//                        headers.remove("sec-ch-ua-platform")

                        // Proceed with the modified request
//                        val url = request?.url?.toString() ?: return null
                        // Create a new request with modified headers
                        val newRequest =
                                object : WebResourceRequest {
                                    override fun getUrl(): Uri = request.url
                                    override fun isForMainFrame(): Boolean = request.isForMainFrame
                                    override fun hasGesture(): Boolean = request.hasGesture()
                                    override fun isRedirect(): Boolean = request.isRedirect
                                    override fun getMethod(): String = request.method
                                    override fun getRequestHeaders(): Map<String, String> = headers
                                }

                        // Proceed with the modified request
                        return super.shouldInterceptRequest(view, newRequest)
                    }

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
//        val LOCATION_PERMISSION_REQUEST_CODE = 100
//        // Request location permissions
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
//                            PackageManager.PERMISSION_GRANTED
//            ) {
//                ActivityCompat.requestPermissions(
//                        this,
//                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                        LOCATION_PERMISSION_REQUEST_CODE
//                )
//            }
//        }

        //        val customUserAgent = WebSettings.getDefaultUserAgent(this).replace("wv", "")
        //        webView.settings.userAgentString = customUserAgent

        //        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile)
        // AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36"
//        webView.settings.setGeolocationEnabled(true)
//        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
//        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7a Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/128.0.6613.127 Mobile Safari/537.36"

//        CookieManager.getInstance().setAcceptCookie(true)
//        webView.settings.domStorageEnabled = true
//
//        webView.settings.useWideViewPort = true
//        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        val headersMap = headers?.keySet()?.associateWith { headers.getString(it) } ?: emptyMap()

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
}
