package com.opacitylabs.opacitycore

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import org.json.JSONObject

class WebViewBrowserActivity : BaseBrowserActivity() {
    private lateinit var webView: WebView

    inner class OpacityJsBridge {
        @JavascriptInterface
        fun onInterceptedRequest(json: String) {
            if (!interceptExtensionEnabled) return
            try {
                val requestData = JSONObject(json)
                emitInterceptedRequest(requestData)
            } catch (e: Exception) {
                Log.e("Opacity SDK", "Error parsing intercepted request", e)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun setupBrowser(container: LinearLayout, headers: Bundle?, interceptEnabled: Boolean) {
        // Clear cookies for private-mode-like behavior
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().setAcceptCookie(true)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)

            addJavascriptInterface(OpacityJsBridge(), "OpacityNative")
        }

        val customUserAgent = headers?.getString("user-agent")
        if (customUserAgent != null) {
            webView.settings.userAgentString = customUserAgent
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                currentUrl = url
                addToVisitedUrls(url)
                emitNavigationEvent()
                return false
            }

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: android.graphics.Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)
                if (url != null) {
                    currentUrl = url
                    addToVisitedUrls(url)
                }
                if (interceptExtensionEnabled) {
                    view?.evaluateJavascript(INTERCEPT_SCRIPT, null)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null) {
                    currentUrl = url
                    updateCookiesFromCookieManager(url)
                }

                view?.evaluateJavascript("document.documentElement.outerHTML") { rawResult ->
                    if (rawResult != null && rawResult != "null") {
                        htmlBody = unescapeJsString(rawResult)
                        emitNavigationEvent()
                        htmlBody = ""
                    } else {
                        emitNavigationEvent()
                    }
                }
            }

            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean
            ) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (url != null) {
                    addToVisitedUrls(url)
                    emitLocationEvent(url)
                }
            }
        }

        val webViewLayoutParams = LinearLayout.LayoutParams(
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

        webView.layoutParams = webViewLayoutParams
        container.addView(webView)
    }

    override fun loadInitialUrl(url: String, headers: Bundle?) {
        val headerMap = mutableMapOf<String, String>()
        headers?.keySet()?.forEach { key ->
            if (key != "user-agent") {
                headers.getString(key)?.let { headerMap[key] = it }
            }
        }
        webView.loadUrl(url, headerMap)
    }

    override fun navigateToUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun onCookiesRequested() {
        updateCookiesFromCookieManager(currentUrl)
    }

    override fun onCookiesForDomainRequested(domain: String) {
        updateCookiesFromCookieManager(currentUrl)
        updateCookiesFromCookieManager("https://$domain")
    }

    override fun cleanupBrowser() {
        CookieManager.getInstance().removeAllCookies(null)
        webView.destroy()
    }

    private fun updateCookiesFromCookieManager(url: String) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) return
            val domain = java.net.URL(url).host
            val cookieString = CookieManager.getInstance().getCookie(url) ?: return
            val cookieDict = JSONObject()
            cookieString.split(";").forEach { part ->
                val trimmed = part.trim()
                val eqIdx = trimmed.indexOf('=')
                if (eqIdx > 0) {
                    val name = trimmed.substring(0, eqIdx).trim()
                    val value = trimmed.substring(eqIdx + 1).trim()
                    cookieDict.put(name, value)
                }
            }
            cookies[domain] =
                cookies[domain]?.let { existing ->
                    JsonUtils.mergeJsonObjects(existing, cookieDict)
                } ?: cookieDict
        } catch (e: Exception) {
            Log.e("Opacity SDK", "Error updating cookies from CookieManager", e)
        }
    }

    private fun unescapeJsString(raw: String): String {
        var s = raw
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
        }
        return s
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\u003C", "<")
            .replace("\\u003E", ">")
            .replace("\\u0026", "&")
            .replace("\\u003c", "<")
            .replace("\\u003e", ">")
            .replace("\\u0026", "&")
    }

    companion object {
        private const val INTERCEPT_SCRIPT = """
(function() {
    const log = (requestType, data) => { try { OpacityNative.onInterceptedRequest(JSON.stringify({ request_type: requestType, data })); } catch(e) {} };

    const nativeToString = Function.prototype.toString;
    const nativeCallToString = Function.prototype.call.bind(nativeToString);
    const wrappedFns = new WeakMap();

    Function.prototype.toString = function() {
        if (wrappedFns.has(this)) {
            return wrappedFns.get(this);
        }
        return nativeCallToString(this);
    };
    wrappedFns.set(Function.prototype.toString, 'function toString() { [native code] }');

    const originalFetch = window.fetch;
    const wrappedFetch = function fetch(input, init) {
        const method = (init && init.method) || (typeof input === 'string' ? 'GET' : input.method || 'GET');
        const url = typeof input === 'string' ? input : input.url;
        if (method.toUpperCase() === 'POST' && init?.body && url.indexOf('/v2/submit-form') !== -1) {
            var bodyStr = typeof init.body === 'string' ? init.body : JSON.stringify(init.body);
            var fullUrl = new URL(url, location.href).href;
            try { OpacityNative.storePostBody(fullUrl, bodyStr); } catch(e) {}
        }
        let requestHeaders = init?.headers || {};
        if (requestHeaders instanceof Headers) requestHeaders = Object.fromEntries(requestHeaders.entries());
        log('fetch_request', { url, method, headers: requestHeaders, body: init?.body });
        return originalFetch.apply(this, arguments).then(function(response) {
            const cloned = response.clone();
            let responseHeaders = cloned.headers || {};
            if (responseHeaders instanceof Headers) responseHeaders = Object.fromEntries(responseHeaders.entries());
            cloned.text().then(function(body) {
                log('fetch_response', { url, method, headers: responseHeaders, body, status: cloned.status });
            });
            return response;
        });
    };
    wrappedFns.set(wrappedFetch, 'function fetch() { [native code] }');
    Object.defineProperty(window, 'fetch', { value: wrappedFetch, writable: true, configurable: true });

    const OriginalXHR = window.XMLHttpRequest;
    const xhrProto = OriginalXHR.prototype;
    const originalOpen = xhrProto.open;
    const originalSend = xhrProto.send;
    const originalSetHeader = xhrProto.setRequestHeader;
    const xhrData = new WeakMap();

    xhrProto.open = function(method, url) {
        xhrData.set(this, { method, url, headers: {} });
        return originalOpen.apply(this, arguments);
    };
    wrappedFns.set(xhrProto.open, 'function open() { [native code] }');

    xhrProto.setRequestHeader = function(name, value) {
        const data = xhrData.get(this);
        if (data) data.headers[name] = value;
        return originalSetHeader.apply(this, arguments);
    };
    wrappedFns.set(xhrProto.setRequestHeader, 'function setRequestHeader() { [native code] }');

    xhrProto.send = function(body) {
        const data = xhrData.get(this);
        if (data && data.method && data.method.toUpperCase() === 'POST' && body && data.url.indexOf('/v2/submit-form') !== -1) {
            var bodyStr = typeof body === 'string' ? body : JSON.stringify(body);
            var fullUrl = new URL(data.url, location.href).href;
            try { OpacityNative.storePostBody(fullUrl, bodyStr); } catch(e) {}
        }
        if (data) {
            log('xhr_request', { method: data.method, url: data.url, headers: data.headers, body });
            this.addEventListener('loadend', () => {
                log('xhr_response', { method: data.method, url: data.url, headers: data.headers, body: this.responseText || this.response, status: this.status });
            });
        }
        return originalSend.apply(this, arguments);
    };
    wrappedFns.set(xhrProto.send, 'function send() { [native code] }');

    Object.defineProperty(navigator, 'webdriver', { get: () => undefined, configurable: true });

    const automationProps = ['__webdriver_script_fn', '__driver_evaluate', '__webdriver_evaluate',
        '__selenium_evaluate', '__fxdriver_evaluate', '__driver_unwrapped', '__webdriver_unwrapped',
        '__selenium_unwrapped', '__fxdriver_unwrapped', '_Selenium_IDE_Recorder', '_selenium',
        'calledSelenium', '_WEBDRIVER_ELEM_CACHE', 'ChromeDriverw', 'driver-hierarchical',
        '__nightmare', '__phantomas', '_phantom', 'phantom', 'callPhantom'];
    automationProps.forEach(p => { try { Object.defineProperty(window, p, { get: () => undefined, configurable: true }); } catch(e) {} });

    const OriginalError = Error;
    Error = function(...args) {
        const err = new OriginalError(...args);
        if (err.stack) err.stack = err.stack.replace(/\n.*OpacityNative.*/g, '');
        return err;
    };
    Error.prototype = OriginalError.prototype;
    Object.setPrototypeOf(Error, OriginalError);
})();
"""
    }
}
