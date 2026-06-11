package com.example.network

import android.util.Log
import com.example.data.SettingsManager
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

data class SearchResult(
    val title: String,
    val snippet: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class TavilySearchRequest(
    @Json(name = "api_key") val apiKey: String,
    @Json(name = "query") val query: String,
    @Json(name = "search_depth") val searchDepth: String = "basic",
    @Json(name = "max_results") val maxResults: Int = 3
)

@JsonClass(generateAdapter = true)
data class TavilyResultItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class TavilySearchResponse(
    @Json(name = "results") val results: List<TavilyResultItem>? = null
)

class WebSearchService(private val okHttpClient: OkHttpClient) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val tavilyAdapter = moshi.adapter(TavilySearchResponse::class.java)
    private val tavilyReqAdapter = moshi.adapter(TavilySearchRequest::class.java)

    suspend fun search(query: String, settings: SettingsManager): List<SearchResult> = withContext(Dispatchers.IO) {
        if (settings.searchProvider == SettingsManager.PROVIDER_TAVILY && settings.tavilyApiKey.isNotBlank()) {
            return@withContext searchTavily(query, settings.tavilyApiKey)
        } else {
            return@withContext searchDuckDuckGo(query)
        }
    }

    private fun searchTavily(query: String, apiKey: String): List<SearchResult> {
        try {
            val reqPayload = TavilySearchRequest(apiKey = apiKey, query = query, maxResults = 3)
            val jsonBody = tavilyReqAdapter.toJson(reqPayload)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("WebSearchService", "Tavily returned error: ${response.code}")
                    return searchDuckDuckGo(query) // fallback
                }
                val bodyStr = response.body?.string() ?: return emptyList()
                val parsed = tavilyAdapter.fromJson(bodyStr)
                return parsed?.results?.map {
                    SearchResult(
                        title = it.title ?: "Search Result",
                        snippet = it.content ?: "",
                        url = it.url ?: ""
                    )
                } ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("WebSearchService", "Tavily search failed, falling back to DDG", e)
            return searchDuckDuckGo(query)
        }
    }

    private fun searchDuckDuckGo(query: String): List<SearchResult> {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("WebSearchService", "DuckDuckGo response error: ${response.code}")
                    return emptyList()
                }
                val html = response.body?.string() ?: return emptyList()
                return parseDdgHtml(html)
            }
        } catch (e: Exception) {
            Log.e("WebSearchService", "DuckDuckGo search error", e)
            return emptyList()
        }
    }

    private fun parseDdgHtml(html: String): List<SearchResult> {
        val list = mutableListOf<SearchResult>()
        // Split HTML by web result container blocks
        val blocks = html.split("<div class=\"result")
        for (i in 1 until blocks.size) {
            val block = blocks[i]

            // extract snippet
            val snippetMatch = Regex("""class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).find(block)
            val rawSnippet = snippetMatch?.groupValues?.get(1) ?: continue
            val snippet = cleanHtml(rawSnippet)

            // extract title
            val titleMatch = Regex("""class="result__url"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).find(block)
                ?: Regex("""class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).find(block)
            val title = cleanHtml(titleMatch?.groupValues?.get(1) ?: "Search Result")

            // extract link
            val hrefMatch = Regex("""href="([^"]+)"""").find(block)
            val url = hrefMatch?.groupValues?.get(1) ?: ""

            if (snippet.isNotBlank()) {
                list.add(SearchResult(title, snippet, url))
            }
            if (list.size >= 3) break
        }
        return list
    }

    private fun cleanHtml(input: String): String {
        return input
            .replace(Regex("<[^>]*>"), "") // strip HTML tags
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
            .replace("&ndash;", "-")
            .replace("&mdash;", "-")
            .trim()
    }
}
