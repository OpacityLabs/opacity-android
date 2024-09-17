package com.opacitylabs.opacitycore

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
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

        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        webView.webViewClient =
                object : WebViewClient() {
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
        val LOCATION_PERMISSION_REQUEST_CODE = 100
        // Request location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        //        val customUserAgent = WebSettings.getDefaultUserAgent(this).replace("wv", "")
        //        webView.settings.userAgentString = customUserAgent

        //        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile)
        // AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36"
        webView.settings.setGeolocationEnabled(true)
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        val desktopUserAgent =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        webView.settings.userAgentString = desktopUserAgent

        CookieManager.getInstance().setAcceptCookie(true)
        webView.settings.domStorageEnabled = true

        webView.settings.useWideViewPort = true
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        //        webView.settings.cache(true)

        webView.loadUrl(url)
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
