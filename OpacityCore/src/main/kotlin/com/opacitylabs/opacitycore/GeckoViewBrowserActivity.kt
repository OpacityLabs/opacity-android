package com.opacitylabs.opacitycore

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import java.net.HttpCookie

class GeckoViewBrowserActivity : BaseBrowserActivity() {
    private lateinit var geckoSession: GeckoSession
    private lateinit var geckoView: GeckoView

    override fun setupBrowser(container: LinearLayout, headers: Bundle?, interceptEnabled: Boolean) {
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
                            val defaultDomain = jsonMessage.getString("domain")

                            val lines = receivedCookies.lines().filter { it.isNotBlank() }

                            val parsedCookies = lines.flatMap { HttpCookie.parse(it) }

                            val cookiesByDomain = mutableMapOf<String, JSONObject>()
                            for (cookie in parsedCookies) {
                                val cookieDomain = cookie.domain?.trimStart('.') ?: defaultDomain
                                val cookieDict = cookiesByDomain.getOrPut(cookieDomain) { JSONObject() }

                                cookieDict.put(cookie.name, cookie.value)
                            }

                            for ((domain, cookieDict) in cookiesByDomain) {
                                cookies[domain] =
                                    cookies[domain]?.let { existingCookies ->
                                        JsonUtils.mergeJsonObjects(existingCookies, cookieDict)
                                    } ?: cookieDict
                            }
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
                    Log.e("Opacity SDK", "Error processing extension message", e)
                }

                return super.onMessage(nativeApp, message, sender)
            }
        }

        OpacityCore.setMainMessageDelegate(sharedMessageDelegate)

        if (interceptEnabled) {
            OpacityCore.setInterceptMessageDelegate(sharedMessageDelegate)
        }

        // Create GeckoSession
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

        // Configure GeckoView layout params to account for action bar
        val geckoLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            val actionBarHeight = supportActionBar?.height ?: 0
            if (actionBarHeight == 0) {
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
    }

    override fun loadInitialUrl(url: String, headers: Bundle?) {
        geckoSession.loadUri(url)
    }

    override fun navigateToUrl(url: String) {
        if (geckoSession.isOpen) {
            geckoSession.loadUri(url)
        } else {
            Log.d("Opacity SDK", "Warning: GeckoSession is not open.")
        }
    }

    override fun cleanupBrowser() {
        OpacityCore.setMainMessageDelegate(null)
        OpacityCore.setInterceptMessageDelegate(null)
        geckoSession.close()
    }
}
