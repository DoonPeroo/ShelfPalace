package com.example.shelfpalace.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RottenTomatoesRating(
    val tomatometer: Int? = null,
    val popcornmeter: Int? = null
)

object RottenTomatoesService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun fetchRatings(title: String, releaseYear: String = ""): RottenTomatoesRating = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext RottenTomatoesRating()

        val cleanYear = releaseYear.trim().take(4)
        val candidateSlugs = generateCandidateSlugs(title, cleanYear)

        var foundTomatometer: Int? = null
        var foundPopcornmeter: Int? = null

        for (slug in candidateSlugs) {
            val rating = fetchFromSlug(slug)
            if (rating.tomatometer != null || rating.popcornmeter != null) {
                if (foundTomatometer == null) foundTomatometer = rating.tomatometer
                if (foundPopcornmeter == null) foundPopcornmeter = rating.popcornmeter
                if (foundTomatometer != null && foundPopcornmeter != null) {
                    return@withContext RottenTomatoesRating(foundTomatometer, foundPopcornmeter)
                }
            }
        }

        // Fallback to OMDb API if Tomatometer is still missing
        if (foundTomatometer == null) {
            foundTomatometer = fetchFromOmdb(title, cleanYear)
        }

        RottenTomatoesRating(
            tomatometer = foundTomatometer,
            popcornmeter = foundPopcornmeter
        )
    }

    private fun generateCandidateSlugs(title: String, year: String): List<String> {
        val baseSlug = createSlug(title)
        val candidates = mutableListOf<String>()

        if (baseSlug.isNotBlank()) {
            if (year.isNotBlank()) {
                candidates.add("${baseSlug}_$year")
            }
            candidates.add(baseSlug)

            if (baseSlug.startsWith("the_")) {
                val withoutThe = baseSlug.removePrefix("the_")
                if (withoutThe.isNotBlank()) {
                    if (year.isNotBlank()) {
                        candidates.add("${withoutThe}_$year")
                    }
                    candidates.add(withoutThe)
                }
            }
        }

        return candidates.distinct()
    }

    private fun createSlug(title: String): String {
        return title.lowercase(Locale.US)
            .replace("&", "and")
            .replace(Regex("[^a-z0-9\\s_]"), "")
            .trim()
            .replace(Regex("[\\s_]+"), "_")
    }

    private fun fetchFromSlug(slug: String): RottenTomatoesRating {
        try {
            val url = "https://www.rottentomatoes.com/m/$slug"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val html = response.body?.string() ?: return RottenTomatoesRating()
                return parseRatingsFromHtml(html)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return RottenTomatoesRating()
    }

    fun parseRatingsFromHtml(html: String): RottenTomatoesRating {
        // Critics Score / Tomatometer
        val criticsMatch = Regex(""""criticsScore"\s*:\s*\{[^}]*?"score"\s*:\s*"(\d+)"""").find(html)
            ?: Regex(""""criticsAll"\s*:\s*\{[^}]*?"score"\s*:\s*"(\d+)"""").find(html)
            ?: Regex(""""aggregateRating"\s*:\s*\{[^}]*?"ratingValue"\s*:\s*"(\d+)"""").find(html)

        val tomatometer = criticsMatch?.groupValues?.get(1)?.toIntOrNull()

        // Audience Score / Popcornmeter
        val audienceMatch = Regex(""""audienceScore"\s*:\s*\{[^}]*?"score"\s*:\s*"(\d+)"""").find(html)
            ?: Regex(""""audienceAll"\s*:\s*\{[^}]*?"score"\s*:\s*"(\d+)"""").find(html)

        val popcornmeter = audienceMatch?.groupValues?.get(1)?.toIntOrNull()

        return RottenTomatoesRating(tomatometer = tomatometer, popcornmeter = popcornmeter)
    }

    private fun fetchFromOmdb(title: String, year: String): Int? {
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val yearParam = if (year.isNotBlank()) "&y=$year" else ""
            val url = "https://www.omdbapi.com/?apikey=trilogy&t=$encodedTitle$yearParam"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                if (json.optString("Response") == "True") {
                    val ratings = json.optJSONArray("Ratings") ?: return null
                    for (i in 0 until ratings.length()) {
                        val ratingObj = ratings.getJSONObject(i)
                        if (ratingObj.optString("Source") == "Rotten Tomatoes") {
                            val valStr = ratingObj.optString("Value") // e.g. "86%"
                            return valStr.replace("%", "").trim().toIntOrNull()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
