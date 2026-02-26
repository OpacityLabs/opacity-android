package com.opacitylabs.opacitycore

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject

abstract class BaseBrowserActivity : AppCompatActivity() {
    private val closeReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.opacitylabs.opacitycore.CLOSE_BROWSER") {
                    finish()
                }
            }
        }

    private val cookiesForCurrentURLRequestReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.opacitylabs.opacitycore.GET_COOKIES_FOR_CURRENT_URL") {
                    val receiver = intent.getParcelableExtra<CookieResultReceiver>("receiver")
                    onCookiesRequested()
                    val domain = java.net.URL(currentUrl).host
                    receiver?.onReceiveResult(getMatchedCookies(domain))
                }
            }
        }

    private val cookiesForDomainRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.opacitylabs.opacitycore.GET_COOKIES_FOR_DOMAIN") {
                val receiver = intent.getParcelableExtra<CookieResultReceiver>("receiver")
                var domain = intent.getStringExtra("domain")
                if (domain?.startsWith(".") == true) {
                    domain = domain.substring(1)
                }

                if (domain != null) {
                    onCookiesForDomainRequested(domain)
                    receiver?.onReceiveResult(getMatchedCookies(domain))
                } else {
                    receiver?.onReceiveResult(JSONObject())
                }
            }
        }
    }

    private val changeUrlReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.opacitylabs.opacitycore.CHANGE_URL") {
                    currentUrl = intent.getStringExtra("url")!!
                    navigateToUrl(currentUrl)
                }
            }
        }

    protected var cookies: MutableMap<String, JSONObject> = mutableMapOf()
    protected var htmlBody: String = ""
    protected var currentUrl: String = ""
    protected val visitedUrls = mutableListOf<String>()
    protected var interceptExtensionEnabled = false

    protected fun getMatchedCookies(domain: String): JSONObject {
        val matchedCookies = JSONObject()
        for ((cookieDomain, cookieObject) in cookies) {
            val cleanDomain = cookieDomain.trimStart('.')
            if (domain == cleanDomain || domain.endsWith(".$cleanDomain")) {
                val keys = cookieObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    matchedCookies.put(key, cookieObject.get(key))
                }
            }
        }
        return matchedCookies
    }

    protected fun emitNavigationEvent() {
        val event: MutableMap<String, Any?> =
            mutableMapOf(
                "event" to "navigation",
                "url" to currentUrl,
                "visited_urls" to visitedUrls,
                "id" to System.currentTimeMillis().toString()
            )

        try {
            val domain = java.net.URL(currentUrl).host
            event["cookies"] = cookies[domain]
        } catch (e: Exception) {
            // If the URL is malformed (usually when it is a URI like "uberlogin://blabla")
            // we don't set any cookies
        }

        if (htmlBody != "") {
            event["html_body"] = htmlBody
        }

        OpacityCore.emitWebviewEvent(JSONObject(event).toString())
        clearVisitedUrls()
    }

    protected fun emitLocationEvent(url: String) {
        val event: Map<String, Any?> =
            mapOf(
                "event" to "location_changed",
                "url" to url,
                "id" to System.currentTimeMillis().toString()
            )
        OpacityCore.emitWebviewEvent(JSONObject(event).toString())
    }

    protected fun emitInterceptedRequest(requestData: JSONObject) {
        val event: MutableMap<String, Any?> =
            mutableMapOf(
                "event" to "intercepted_request",
                "request_type" to requestData.optString("request_type"),
                "data" to requestData.opt("data"),
                "id" to System.currentTimeMillis().toString()
            )
        val json = JSONObject(event).toString()
        OpacityCore.emitWebviewEvent(json)
    }

    protected fun onClose() {
        val event: Map<String, Any> =
            mapOf("event" to "close", "id" to System.currentTimeMillis().toString())
        OpacityCore.emitWebviewEvent(JSONObject(event).toString())
        interceptExtensionEnabled = false
        finish()
    }

    protected fun addToVisitedUrls(url: String) {
        if (visitedUrls.isNotEmpty() && visitedUrls.last() == url) {
            return
        }
        visitedUrls.add(url)
    }

    protected fun clearVisitedUrls() {
        visitedUrls.clear()
    }

    /**
     * Called before returning cookies for current URL — subclasses can override
     * to sync cookies from their engine (e.g., WebView's CookieManager). No-op by default.
     */
    protected open fun onCookiesRequested() {}

    /**
     * Called before returning cookies for a specific domain. Default delegates to onCookiesRequested().
     */
    protected open fun onCookiesForDomainRequested(domain: String) {
        onCookiesRequested()
    }

    /** Set up the browser engine and add its view to the container. */
    protected abstract fun setupBrowser(container: LinearLayout, headers: Bundle?, interceptEnabled: Boolean)

    /** Load the initial URL with optional headers. */
    protected abstract fun loadInitialUrl(url: String, headers: Bundle?)

    /** Navigate to a new URL (e.g., from changeUrl broadcast). */
    protected abstract fun navigateToUrl(url: String)

    /** Clean up browser engine resources. */
    protected abstract fun cleanupBrowser()

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle edge-to-edge display for Android 15+
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(
            closeReceiver,
            IntentFilter("com.opacitylabs.opacitycore.CLOSE_BROWSER")
        )
        localBroadcastManager.registerReceiver(
            cookiesForCurrentURLRequestReceiver,
            IntentFilter("com.opacitylabs.opacitycore.GET_COOKIES_FOR_CURRENT_URL")
        )
        localBroadcastManager.registerReceiver(
            cookiesForDomainRequestReceiver,
            IntentFilter("com.opacitylabs.opacitycore.GET_COOKIES_FOR_DOMAIN")
        )
        localBroadcastManager.registerReceiver(
            changeUrlReceiver,
            IntentFilter("com.opacitylabs.opacitycore.CHANGE_URL")
        )

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val closeButton =
            Button(this, null, android.R.attr.buttonStyleSmall).apply {
                text = "\u2715"
                textSize = 18f
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setOnClickListener { onClose() }
            }

        val actionBarLayoutParams =
            ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
        supportActionBar?.setCustomView(closeButton, actionBarLayoutParams)
        supportActionBar?.setDisplayShowCustomEnabled(true)

        interceptExtensionEnabled = intent.getBooleanExtra("enableInterceptRequests", false)

        val headers: Bundle? = intent.getBundleExtra("headers")

        // Create a container layout to properly handle the action bar spacing
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(0, insets.top, 0, insets.bottom)
                windowInsets
            }
        }

        setupBrowser(container, headers, interceptExtensionEnabled)

        setContentView(container)

        val url = intent.getStringExtra("url")!!
        loadInitialUrl(url, headers)
    }

    override fun onDestroy() {
        super.onDestroy()
        val lbm = LocalBroadcastManager.getInstance(this)
        lbm.unregisterReceiver(closeReceiver)
        lbm.unregisterReceiver(cookiesForDomainRequestReceiver)
        lbm.unregisterReceiver(cookiesForCurrentURLRequestReceiver)
        lbm.unregisterReceiver(changeUrlReceiver)
        cleanupBrowser()
        OpacityCore.onBrowserDestroyed()
    }
}
