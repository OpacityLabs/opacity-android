package com.opacitylabs.opacitycore

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension

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
                    val browserCookies = cookies[domain] ?: JSONObject()
                    receiver?.onReceiveResult(browserCookies)
                }
            }
        }

    private val cookiesForDomainRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.opacitylabs.opacitycore.GET_COOKIES_FOR_DOMAIN") {
                val receiver = intent.getParcelableExtra<CookieResultReceiver>("receiver")
                val domain = intent.getStringExtra("domain")
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

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val layoutParams =
            ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
        supportActionBar?.setCustomView(closeButton, layoutParams)
        supportActionBar?.setDisplayShowCustomEnabled(true)

        OpacityCore.getRuntime()
            .webExtensionController
            .installBuiltIn("resource://android/assets/extension/")
            .accept { ext ->
                ext?.setMessageDelegate(
                    object : WebExtension.MessageDelegate {
                        override fun onMessage(
                            nativeApp: String,
                            message: Any,
                            sender: WebExtension.MessageSender
                        ): GeckoResult<Any>? {
                            val jsonMessage = message as JSONObject

                            Log.d("GECKO MESSAGE: ", message.toString())

                            // This are the messages from our injected JS script to extract
                            // cookies
                            when (jsonMessage.getString("event")) {
                                "window.close" -> {
                                    Log.d("window.close ", "window.close reached!")
                                    val event: Map<String, Any?> =
                                        mapOf(
                                            "event" to "window.close",
                                            "id" to System.currentTimeMillis().toString()
                                        )
                                    OpacityCore.emitWebviewEvent(JSONObject(event).toString())
                                    emitNavigationEvent()
                                }

                                "html_body" -> {
                                    htmlBody = jsonMessage.getString("html")
                                    emitNavigationEvent()
                                }

                                "cookies" -> {
                                    val receivedCookies =
                                        jsonMessage.getJSONObject("cookies")
                                    val domain = jsonMessage.getString("domain")

                                    cookies[domain] =
                                        cookies[domain]?.let { existingCookies ->
                                            JsonUtils.mergeJsonObjects(
                                                receivedCookies,
                                                existingCookies
                                            )
                                        }
                                            ?: receivedCookies
                                }

                                else -> {
                                    // Intentionally left blank
                                }
                            }

                            return super.onMessage(nativeApp, message, sender)
                        }
                    },
                    "gecko"
                )

                geckoSession =
                    GeckoSession().apply {
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
                            return super.onLoadRequest(session, request)
                        }

                        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                            Log.d("onNewSession ", "onNewSession reached")
                            val newSession = GeckoSession().apply {
                                settings.allowJavascript = true
                                navigationDelegate = this@apply.navigationDelegate
                                open(OpacityCore.getRuntime())
                            }
                            geckoView.setSession(newSession)
                            newSession.loadUri(uri)
                            return GeckoResult.fromValue(newSession)
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
                                currentUrl = url
                                addToVisitedUrls(url)
                            }
                        }
                    }

                geckoView = GeckoView(this).apply { setSession(geckoSession) }

                setContentView(geckoView)
                val url = intent.getStringExtra("url")!!

                geckoSession.loadUri(url)
            }
    }

    private fun emitNavigationEvent() {
        val domain = java.net.URL(currentUrl).host
        val event: Map<String, Any?> =
            mapOf(
                "event" to "navigation",
                "url" to currentUrl,
                "html_body" to htmlBody,
                "cookies" to cookies[domain],
                "visited_urls" to visitedUrls,
                "id" to System.currentTimeMillis().toString()
            )
        OpacityCore.emitWebviewEvent(JSONObject(event).toString())
        clearVisitedUrls()
    }

    private fun onClose() {
        val event: Map<String, Any> =
            mapOf("event" to "close", "id" to System.currentTimeMillis().toString())
        OpacityCore.emitWebviewEvent(JSONObject(event).toString())
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
    }
}
