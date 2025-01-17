package com.opacitylabs.opacitycore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime
import kotlinx.serialization.*
import kotlinx.serialization.json.*

object OpacityCore {
    enum class Environment {
        TEST,
        LOCAL,
        STAGING,
        PRODUCTION,
    }

    private lateinit var appContext: Context
    private lateinit var cryptoManager: CryptoManager
    private lateinit var _url: String
    private var headers: Bundle = Bundle()
    private lateinit var sRuntime: GeckoRuntime

    init {
        System.loadLibrary("OpacityCore")
    }

    fun initialize(apiKey: String, dryRun: Boolean, environment: Environment): Int {
        return init(apiKey, dryRun, environment.ordinal)
    }

    fun setContext(context: Context) {
        appContext = context
        sRuntime = GeckoRuntime.create(appContext.applicationContext)
        cryptoManager = CryptoManager(appContext.applicationContext)
    }

    fun getRuntime(): GeckoRuntime {
        if (!::sRuntime.isInitialized) {
            throw IllegalStateException("GeckoRuntime is not initialized. Call initialize() first.")
        }
        return sRuntime
    }

    fun securelySet(key: String, value: String) {
        cryptoManager.set(key, value)
    }

    fun securelyGet(key: String): String? {
        return cryptoManager.get(key)
    }

    fun prepareInAppBrowser(url: String) {
        headers = Bundle()
        _url = url
    }

    fun setBrowserHeader(key: String, value: String) {
        headers.putString(key, value)
    }

    fun presentBrowser() {
        val intent = Intent()
        intent.setClassName(
            appContext.packageName,
            "com.opacitylabs.opacitycore.InAppBrowserActivity"
        )
        intent.putExtra("url", _url)
        intent.putExtra("headers", headers)
        appContext.startActivity(intent)
    }

    fun closeBrowser() {
        val closeIntent = Intent("com.opacitylabs.opacitycore.CLOSE_BROWSER")
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(closeIntent)
    }

//    suspend fun getRedditAccount(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getRedditAccountNative() }
//    }
//
//    suspend fun getRedditFollowedSubreddits(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getRedditFollowedSubredditsNative() }
//    }
//
//    suspend fun getRedditComments(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getRedditCommentsNative() }
//    }
//
//    suspend fun getRedditPosts(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getRedditPostsNative() }
//    }
//
//    suspend fun getZabkaAccount(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getZabkaAccountNative() }
//    }
//
//    suspend fun getZabkaPoints(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getZabkaPointsNative() }
//    }
//
//    private external fun init(apiKey: String, dryRun: Boolean): Int
//    suspend fun getCartaProfile(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getCartaProfileNative() }
//    }
//
//    suspend fun getCartaOrganizations(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getCartaOrganizationsNative() }
//    }
//
//    suspend fun getCartaPortfolioInvestments(firmId: String, accountId: String): OpacityResponse {
//        return withContext(Dispatchers.IO) { getCartaPortfolioInvestmentsNative(firmId, accountId) }
//    }
//
//    suspend fun getCartaHoldingsCompanies(accountId: String): OpacityResponse {
//        return withContext(Dispatchers.IO) { getCartaHoldingsCompaniesNative(accountId) }
//    }
//
//    suspend fun getCartaCorporationSecurities(
//        accountId: String,
//        corporationId: String
//    ): OpacityResponse {
//        return withContext(Dispatchers.IO) {
//            getCartaCorporationSecuritiesNative(
//                accountId,
//                corporationId
//            )
//        }
//    }
//
//    suspend fun getGithubProfile(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getGithubProfileNative() }
//    }
//
//    suspend fun getInstagramProfile(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getInstagramProfileNative() }
//    }
//
//    suspend fun getInstagramLikes(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getInstagramLikesNative() }
//    }
//
//    suspend fun getInstagramComments(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getInstagramCommentsNative() }
//    }
//
//    suspend fun getInstagramSavedPosts(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getInstagramSavedPostsNative() }
//    }
//
//    suspend fun getGustoMembersTable(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getGustoMembersTableNative() }
//    }
//
//    suspend fun getGustoPayrollAdminId(): OpacityResponse {
//        return withContext(Dispatchers.IO) { getGustoPayrollAdminIdNative() }
//    }

    private fun parseJsonElementToAny(jsonElement: JsonElement): Any {
        return when (jsonElement) {
            is JsonPrimitive -> {
                when {
                    jsonElement.isString -> jsonElement.content
                    jsonElement.intOrNull != null -> jsonElement.int
                    jsonElement.booleanOrNull != null -> jsonElement.boolean
                    jsonElement.doubleOrNull != null -> jsonElement.double
                    else -> throw Exception("Could not convert JSON primitive $jsonElement")
                }
            }
            is JsonObject -> {
                jsonElement.toMap().mapValues { parseJsonElementToAny(it.value) }
            }
            is JsonArray -> {
                jsonElement.map { parseJsonElementToAny(it) }
            }
            else -> throw Exception("Could not convert JSON primitive $jsonElement")
        }
    }

    suspend fun get(name: String, params: Map<String, Any>?): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            val paramsString = params?.let { Json.encodeToString(it) }
            val res = getNative(name, paramsString)
            if(res.status != 0) {
                throw Exception(res.err)
            }

            val map: Map<String, Any> = Json.parseToJsonElement(res.data!!).jsonObject
                .mapValues { parseJsonElementToAny(it.value) }
            map
        }
    }

    private external fun init(apiKey: String, dryRun: Boolean, environment: Int): Int

    private external fun getNative(name: String, params: String?): OpacityResponse

    external fun emitWebviewEvent(eventJson: String)

//    private external fun getRedditAccountNative(): OpacityResponse
//    private external fun getRedditFollowedSubredditsNative(): OpacityResponse
//    private external fun getRedditCommentsNative(): OpacityResponse
//    private external fun getRedditPostsNative(): OpacityResponse
//
//    // Zabka
//    private external fun getZabkaAccountNative(): OpacityResponse
//    private external fun getZabkaPointsNative(): OpacityResponse
//
//    // carta
//    private external fun getCartaProfileNative(): OpacityResponse
//    private external fun getCartaOrganizationsNative(): OpacityResponse
//    private external fun getCartaPortfolioInvestmentsNative(
//        firmId: String,
//        accountId: String
//    ): OpacityResponse
//
//    private external fun getCartaHoldingsCompaniesNative(accountId: String): OpacityResponse
//    private external fun getCartaCorporationSecuritiesNative(
//        accountId: String,
//        corporationId: String
//    ): OpacityResponse
//
//    // github
//    private external fun getGithubProfileNative(): OpacityResponse
//
//    // instagram
//    private external fun getInstagramProfileNative(): OpacityResponse
//    private external fun getInstagramLikesNative(): OpacityResponse
//    private external fun getInstagramCommentsNative(): OpacityResponse
//    private external fun getInstagramSavedPostsNative(): OpacityResponse
//
//    // Gusto
//    private external fun getGustoMembersTableNative(): OpacityResponse
//    private external fun getGustoPayrollAdminIdNative(): OpacityResponse
}
