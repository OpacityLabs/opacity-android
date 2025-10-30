package com.opacitylabs.opacitycore

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import java.net.HttpCookie

class InAppBrowserActivity : AppCompatActivity() {
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
                if (intent?.action == "com.opacitylabs.opacitycore.GET_COOKIES_FOR_CURRENT_URL"
                ) {
                    val receiver = intent.getParcelableExtra<CookieResultReceiver>("receiver")
                    val domain = java.net.URL(currentUrl).host
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
                    receiver?.onReceiveResult(matchedCookies)
                }
            }
        }

    private val cookiesForDomainRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.opacitylabs.opacitycore.GET_COOKIES_FOR_DOMAIN") {
                val receiver = intent.getParcelableExtra<CookieResultReceiver>("receiver")
                var domain = intent.getStringExtra("domain")
                if (domain?.startsWith(".") == true) {
                    // If the domain starts with a dot, we have to remove it as per rfc 6265
                    //  https://datatracker.ietf.org/doc/html/rfc6265#section-5.2.3
                    domain = domain.substring(1)
                }

                val browserCookies = cookies[domain] ?: JSONObject()
                receiver?.onReceiveResult(browserCookies)
            }
        }
    }

    private lateinit var geckoSession: GeckoSession
    private lateinit var geckoView: GeckoView
    private var cookies: MutableMap<String, JSONObject> = mutableMapOf()
    private var htmlBody: String = ""
    private var currentUrl: String = ""
    private val visitedUrls = mutableListOf<String>()
    private var interceptExtensionEnabled = false

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle edge-to-edge display for Android 15+
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Enable edge-to-edge
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

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val closeButton =
            Button(this, null, android.R.attr.buttonStyleSmall).apply {
                text = "✕"
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

        val controller = OpacityCore.getRuntime().webExtensionController
        interceptExtensionEnabled = intent.getBooleanExtra("enableInterceptRequests", false)

        // Create shared message delegate for both extensions
        val sharedMessageDelegate = object : WebExtension.MessageDelegate {
            override fun onMessage(
                nativeApp: String,
                message: Any,
                sender: WebExtension.MessageSender
            ): GeckoResult<Any>? {
                try {
                    val jsonMessage = message as JSONObject
                    when (jsonMessage.getString("event")) {
                        "html_body" -> {
                            htmlBody = jsonMessage.getString("html")
                            emitNavigationEvent()

                            // clear the html_body, needed so we stay consistent with iOS
                            htmlBody = ""
                        }

                        "cookies" -> {
                            val receivedCookies = jsonMessage.getString("cookies")
                            var domain = jsonMessage.getString("domain")

                            val lines = receivedCookies.lines().filter { it.isNotBlank() }

                            val parsedCookies = lines.flatMap { HttpCookie.parse(it) }
                            var cookieDict = JSONObject()

                            for (cookie in parsedCookies) {
                                val cookieDomain = cookie.domain

                                if (cookieDomain != null) {
                                    domain = cookieDomain
                                }

                                cookieDict.put(cookie.name, cookie.value)
                            }

                            cookies[domain] =
                                cookies[domain]?.let { existingCookies ->
                                    JsonUtils.mergeJsonObjects(existingCookies, cookieDict)
                                } ?: cookieDict
                        }       

                        "intercepted_request" -> {
                            if (interceptExtensionEnabled) {
                                val requestData = jsonMessage.optJSONObject("data")
                                if (requestData != null) {
                                    emitInterceptedRequest(requestData)
                                }
                            }
                        }


                        else -> {
                            // Intentionally left blank
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error processing extension message", e)
                }

                return super.onMessage(nativeApp, message, sender)
            }
        }

        // Install main extension
        controller.installBuiltIn("resource://android/assets/extension/")
            .accept { ext ->
                ext?.setMessageDelegate(sharedMessageDelegate, "gecko")
            }

        // Install intercept extension if enabled
        if (interceptExtensionEnabled) {
            controller.installBuiltIn("resource://android/assets/interceptExtension/")
                .accept { ext ->
                    ext?.setMessageDelegate(sharedMessageDelegate, "gecko")
                }
        }

        // Create GeckoSession only once
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(true)
            .useTrackingProtection(true)
            .allowJavascript(true)
            .build()

        geckoSession =
            GeckoSession(settings).apply {
                setContentDelegate(object : ContentDelegate {})
                open(OpacityCore.getRuntime())
            }

        geckoSession.settings.apply { allowJavascript = true }

        val headers: Bundle? = intent.getBundleExtra("headers")
        val customUserAgent = headers?.getString("user-agent")
        if (customUserAgent != null) {
            geckoSession.settings.userAgentOverride = customUserAgent
        }

        geckoSession.navigationDelegate =
            object : GeckoSession.NavigationDelegate {
                override fun onLoadRequest(
                    session: GeckoSession,
                    request: GeckoSession.NavigationDelegate.LoadRequest
                ): GeckoResult<AllowOrDeny>? {
                    currentUrl = request.uri
                    addToVisitedUrls(request.uri)

                    emitNavigationEvent()

                    return super.onLoadRequest(session, request)
                }

                override fun onLocationChange(
                    session: GeckoSession,
                    url: String?,
                    perms:
                    MutableList<
                            GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean
                ) {
                    if (url != null) {
                        addToVisitedUrls(url)
                        emitLocationEvent(url)
                    }
                }
            }

        geckoView = GeckoView(this).apply { setSession(geckoSession) }

        // Create a container layout to properly handle the action bar spacing
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Handle window insets for the container
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(0, insets.top, 0, insets.bottom)
                windowInsets
            }
        }

        // Configure GeckoView layout params to account for action bar
        val geckoLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            // Add top margin to account for action bar height
            val actionBarHeight = supportActionBar?.height ?: 0
            if (actionBarHeight == 0) {
                // Fallback to standard action bar height
                val typedArray =
                    theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
                topMargin = typedArray.getDimensionPixelSize(0, 0)
                typedArray.recycle()
            } else {
                topMargin = actionBarHeight
            }
        }

        geckoView.layoutParams = geckoLayoutParams
        container.addView(geckoView)

        setContentView(container)
        val url = intent.getStringExtra("url")!!

        geckoSession.loadUri(url)
    }

    private fun emitInterceptedRequest(requestData: JSONObject) {
        val event: MutableMap<String, Any?> =
            mutableMapOf(
                "event" to "intercepted_request",
                "request_type" to requestData.optString("request_type"),
                "data" to requestData.opt("data"),
                "id" to System.currentTimeMillis().toString()
            )
        OpacityCore.emitWebviewEvent(JSONObject(event).toString())
    }
    private fun emitLocationEvent(url: String) {
        val event: Map<String, Any?> =
            mapOf(
                "event" to "location_changed",
                "url" to url,
                "id" to System.currentTimeMillis().toString()
            )
        OpacityCore.emitWebviewEvent(JSONObject(event).toString())
    }

    private fun emitNavigationEvent() {
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

    private fun onClose() {
        val event: Map<String, Any> =
            mapOf("event" to "close", "id" to System.currentTimeMillis().toString())
        OpacityCore.emitWebviewEvent(JSONObject(event).toString())
        interceptExtensionEnabled = false
        finish()
    }

    private fun addToVisitedUrls(url: String) {
        if (visitedUrls.isNotEmpty() && visitedUrls.last() == url) {
            return
        }
        visitedUrls.add(url)
    }

    private fun clearVisitedUrls() {
        visitedUrls.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(cookiesForDomainRequestReceiver)
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(cookiesForCurrentURLRequestReceiver)
        geckoSession.close()
        OpacityCore.onBrowserDestroyed()
    }
}
