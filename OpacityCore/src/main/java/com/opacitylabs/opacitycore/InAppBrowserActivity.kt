package com.opacitylabs.opacitycore

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoView


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

//        geckoSession = GeckoSession().apply {
//            open(GeckoSessionSettings())
//            setCallback(object : GeckoSession.Callback() {
//                override fun onPageLoadEnd(session: GeckoSession?, uri: Uri?) {
//                    uri?.let { handleNavigation(it.toString()) }
//                }
//
//                override fun onNavigation(uri: Uri?) {
//                    uri?.let { handleNavigation(it.toString()) }
//                }
//            })
//        }

        // Initialize GeckoRuntime before using it
        if (!::sRuntime.isInitialized) {
            sRuntime = GeckoRuntime.create(this)
        }

        geckoSession = GeckoSession().apply {
            setContentDelegate(object : ContentDelegate {})
            open(sRuntime)
        }

        geckoView = GeckoView(this).apply {
            setSession(geckoSession)
        }
//
        setContentView(geckoView)
//
        val url = intent.getStringExtra("url") ?: return
//        val headers = intent.getBundleExtra("headers")
//
//        val webRequestHeaders = WebRequestHeaders().apply {
//            (headers?.keySet()?.associateWith { headers.getString(it) } ?: emptyMap()).forEach { (key, value) ->
//                setHeader(key, value)
//            }
//            setHeader("X-Requested-With", "")
//        }

        geckoSession.loadUri(url)
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
        // Implement cookie extraction for GeckoView if necessary
        return emptyMap() // Placeholder
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver)
        geckoSession.close()
    }
}
