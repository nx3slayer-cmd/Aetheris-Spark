package com.kallistocore.ai.domain.search

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class SearchSnippet(
    val title: String,
    val snippet: String,
    val url: String
)

@Serializable
data class SearchToolResult(
    val query: String,
    val snippets: List<SearchSnippet>,
    val appMatches: List<String> = emptyList(),
    val rawSummary: String = ""
)

class DeviceSearchController(private val context: Context) {

    private val httpClient = HttpClient(OkHttp)

    /**
     * Searches installed applications on the Android phone.
     */
    fun searchInstalledApps(query: String): List<String> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        
        return apps.mapNotNull { resolveInfo ->
            val appName = resolveInfo.loadLabel(pm).toString()
            if (appName.contains(query, ignoreCase = true)) {
                appName
            } else null
        }
    }

    /**
     * Launches an app directly or triggers an Android search intent.
     */
    fun launchAppByName(name: String): Boolean {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val target = apps.find { it.loadLabel(pm).toString().equals(name, ignoreCase = true) }

        return if (target != null) {
            val launchIntent = pm.getLaunchIntentForPackage(target.activityInfo.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                true
            } else false
        } else false
    }

    /**
     * Opens an external search query directly in the device default browser.
     */
    fun openSearchInBrowser(query: String) {
        val encodedQuery = Uri.encode(query)
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://duckduckgo.com/?q=$encodedQuery")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    }

    /**
     * Executes an on-device web scrape and formats snippets into LLM-friendly context.
     */
    suspend fun executeSearch(query: String): SearchToolResult = withContext(Dispatchers.IO) {
        val appMatches = searchInstalledApps(query)
        val snippets = mutableListOf<SearchSnippet>()

        try {
            val encoded = query.encodeURLParameter()
            val response = httpClient.get("https://html.duckduckgo.com/html/?q=$encoded") {
                header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/114.0 Firefox/114.0")
            }

            if (response.status.isSuccess()) {
                val html = response.bodyAsText()
                
                // Parse lightweight search result snippets
                val resultRegex = Regex("""<a class="result__snippet[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
                val titleRegex = Regex("""<a class="result__url[^>]*href="([^"]*)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)

                val bodyMatches = resultRegex.findAll(html).take(4).toList()
                val titleMatches = titleRegex.findAll(html).take(4).toList()

                for (i in bodyMatches.indices) {
                    val cleanSnippet = bodyMatches[i].groupValues[1]
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&quot;", "\"")
                        .replace("&amp;", "&")
                        .trim()

                    val cleanUrl = if (i < titleMatches.size) titleMatches[i].groupValues[1] else ""
                    val cleanTitle = if (i < titleMatches.size) {
                        titleMatches[i].groupValues[2].replace(Regex("<[^>]*>"), "").trim()
                    } else "Web Result ${i + 1}"

                    if (cleanSnippet.isNotBlank()) {
                        snippets.add(SearchSnippet(cleanTitle, cleanSnippet, cleanUrl))
                    }
                }
            }
        } catch (_: Exception) {
            // Offline fallback
        }

        val summary = buildString {
            if (appMatches.isNotEmpty()) {
                append("Device Apps Found: ").append(appMatches.joinToString(", ")).append("\n")
            }
            if (snippets.isNotEmpty()) {
                append("Live Web Search Context:\n")
                snippets.forEachIndexed { idx, s ->
                    append("[${idx + 1}] ${s.title}: ${s.snippet}\n")
                }
            }
        }

        SearchToolResult(
            query = query,
            snippets = snippets,
            appMatches = appMatches,
            rawSummary = summary
        )
    }
}
