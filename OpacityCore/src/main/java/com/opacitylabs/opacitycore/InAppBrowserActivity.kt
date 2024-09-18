package com.opacitylabs.opacitycore

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebRequestError


//import org.mozilla.geckoview.WebRequestCallback
//import org.mozilla.geckoview.WebRequestData
//import org.mozilla.geckoview.WebRequestHeaders

class InAppBrowserActivity : AppCompatActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.opacitylabs.opacitycore.CLOSE_BROWSER") {
                finish()
            }
        }
    }

    private lateinit var sRuntime: GeckoRuntime
    private lateinit var geckoSession: GeckoSession
    private lateinit var geckoView: GeckoView

    @SuppressLint("WrongThread")

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

        if (!::sRuntime.isInitialized) {
            sRuntime = GeckoRuntime.create(this)
        }

        sRuntime.webExtensionController.installBuiltIn("resource://android/assets/extension/").accept { ext ->
            if (ext != null) {
                Log.d("InAppBrowserActivity", "WebExtension successfully installed")
                // Set the MessageDelegate or do further initialization here
            } else {
                Log.e("InAppBrowserActivity", "Failed to install WebExtension")
            }
            ext?.setMessageDelegate(
            object : WebExtension.MessageDelegate {
                override fun onMessage(
                    nativeApp: String,
                    message: Any,
                    sender: WebExtension.MessageSender
                ): GeckoResult<Any>? {
                    Log.d("InAppBrowserActivity", "🟩 Message received ${message}")
//                     if (message.event == "cookies") {
//                        val cookies = message.data["cookies"] as List<Map<String, String>>
//                        val json = "{\"event\": \"cookies\", \"cookies\": ${convertToJsonString(cookies)}, \"id\": \"${System.currentTimeMillis()}\"}"
//                        OpacityCore.emitWebviewEvent(json)
//                    }
                    return super.onMessage(nativeApp, message, sender)
                }
//                override fun onMessage(
//                    extension: GeckoWebExtension,
//                    message: GeckoWebExtension.Message
//                ) {
//                    // Handle the cookies received from the web extension
//                    if (message.event == "cookies") {
//                        val cookies = message.data["cookies"] as List<Map<String, String>>
//                        val json = "{\"event\": \"cookies\", \"cookies\": ${convertToJsonString(cookies)}, \"id\": \"${System.currentTimeMillis()}\"}"
//                        OpacityCore.emitWebviewEvent(json)
//                    }
//                }
            }, "gecko")

            geckoSession = GeckoSession().apply {
                setContentDelegate(object : ContentDelegate {})
                open(sRuntime)
            }

            geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onLocationChange(
                    session: GeckoSession,
                    url: String?,
                    perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean
                ) {
                    if(url != null) {
                        handleNavigation(url)
                    }
                }
            }

            geckoView = GeckoView(this).apply {
                setSession(geckoSession)
            }

            setContentView(geckoView)
            val url = intent.getStringExtra("url")!!

            geckoSession.loadUri(url)
        }


    }

    private fun handleNavigation(url: String) {
        // Assuming `OpacityCore.emitWebviewEvent` is available
        val cookies = extractCookies(url)
        val json =
            "{\"event\": \"navigation\"," +
                    "\"url\": \"$url\", \"cookies\": ${convertToJsonString(cookies)}, \"id\": \"${
                        System.currentTimeMillis().toString()
                    }\"}"
        OpacityCore.emitWebviewEvent(json)
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

    private fun extractDomainFromUrl(url: String): String {
        val uri = Uri.parse(url)
        return uri.host ?: ""
    }

    private fun extractCookies(url: String): Map<String, String> {
//        geckoSession.evaluate("document.cookie;").then(result -> {
//            // Process cookies here
//            String cookies = result.toString();
//            // Send cookies back to your app
//        }).catch(e -> {
//            // Handle error
//        });
//
        // Implement cookie extraction for GeckoView if necessary
        return emptyMap() // Placeholder
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver)
        geckoSession.close()
    }
}
