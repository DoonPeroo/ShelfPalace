package com.example.shelfpalace.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class MetacriticRating(
    val criticScore: Double? = null,
    val userScore: Double? = null
)

object MetacriticService {
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

    suspend fun fetchRatings(title: String, platformId: String? = null): MetacriticRating = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext MetacriticRating()

        val candidateSlugs = generateCandidateSlugs(title, platformId)

        var foundCritic: Double? = null
        var foundUser: Double? = null

        for (slug in candidateSlugs) {
            val rating = fetchFromSlug(slug)
            if (rating.criticScore != null || rating.userScore != null) {
                if (foundCritic == null) foundCritic = rating.criticScore
                if (foundUser == null) foundUser = rating.userScore
                if (foundCritic != null && foundUser != null) {
                    return@withContext MetacriticRating(foundCritic, foundUser)
                }
            }
        }

        if (foundCritic == null || foundUser == null) {
            val searchSlug = searchForGameSlug(title)
            if (searchSlug != null && !candidateSlugs.contains(searchSlug)) {
                val rating = fetchFromSlug(searchSlug)
                if (foundCritic == null) foundCritic = rating.criticScore
                if (foundUser == null) foundUser = rating.userScore
            }
        }

        MetacriticRating(
            criticScore = foundCritic,
            userScore = foundUser
        )
    }

    private fun generateCandidateSlugs(title: String, platformId: String?): List<String> {
        val baseSlug = createSlug(title)
        val candidates = mutableListOf<String>()

        if (baseSlug.isNotBlank()) {
            val platformSlug = getMetacriticPlatformSlug(platformId)
            if (platformSlug != null) {
                candidates.add("$baseSlug-$platformSlug")
            }
            candidates.add(baseSlug)

            if (baseSlug.startsWith("the-")) {
                val withoutThe = baseSlug.removePrefix("the-")
                if (withoutThe.isNotBlank()) {
                    if (platformSlug != null) {
                        candidates.add("$withoutThe-$platformSlug")
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
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("[\\s_]+"), "-")
    }

    private fun fetchFromSlug(slug: String): MetacriticRating {
        try {
            val url = "https://www.metacritic.com/game/$slug/"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return MetacriticRating()
            }

            val html = response.body?.string() ?: ""
            response.close()

            if (html.isBlank()) return MetacriticRating()

            var critic: Double? = null
            var user: Double? = null

            // Pattern 1: title="Metascore XX out of 100" or aria-label="Metascore XX out of 100"
            val metascoreMatcher = Pattern.compile("(?:title|aria-label)=\"Metascore\\s+([0-9]+)\\s+out of 100\"", Pattern.CASE_INSENSITIVE).matcher(html)
            if (metascoreMatcher.find()) {
                critic = metascoreMatcher.group(1)?.toDoubleOrNull()
            }

            // Fallback for critic score: schema.org "ratingValue": XX
            if (critic == null) {
                val schemaMatcher = Pattern.compile("\"ratingValue\"\\s*:\\s*\"?([0-9]+)\"?", Pattern.CASE_INSENSITIVE).matcher(html)
                if (schemaMatcher.find()) {
                    critic = schemaMatcher.group(1)?.toDoubleOrNull()
                }
            }

            // Pattern 2: title="User score X.X out of 10" or aria-label="User score X.X out of 10"
            val userScoreMatcher = Pattern.compile("(?:title|aria-label)=\"User\\s+score\\s+([0-9]+(?:\\.[0-9]+)?)\\s+out of 10\"", Pattern.CASE_INSENSITIVE).matcher(html)
            if (userScoreMatcher.find()) {
                user = userScoreMatcher.group(1)?.toDoubleOrNull()
            }

            return MetacriticRating(criticScore = critic, userScore = user)
        } catch (e: Exception) {
            e.printStackTrace()
            return MetacriticRating()
        }
    }

    private fun searchForGameSlug(title: String): String? {
        try {
            val encodedQuery = URLEncoder.encode(title, "UTF-8")
            val url = "https://www.metacritic.com/search/$encodedQuery/"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }

            val html = response.body?.string() ?: ""
            response.close()

            val linkMatcher = Pattern.compile("href=\"/game/([^\"/]+)/?\"").matcher(html)
            if (linkMatcher.find()) {
                return linkMatcher.group(1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getMetacriticPlatformSlug(platformId: String?): String? {
        if (platformId == null) return null
        return when (platformId) {
            "nintendo_switch" -> "switch"
            "nintendo_3ds" -> "3ds"
            "nintendo_ds" -> "ds"
            "nintendo_wiiu" -> "wii-u"
            "nintendo_wii" -> "wii"
            "nintendo_gamecube" -> "gamecube"
            "nintendo_n64" -> "nintendo-64"
            "nintendo_snes" -> "snes"
            "nintendo_nes" -> "nes"
            "sony_ps5" -> "playstation-5"
            "sony_ps4" -> "playstation-4"
            "sony_ps3" -> "playstation-3"
            "sony_ps2" -> "playstation-2"
            "sony_ps1" -> "playstation"
            "sony_psp" -> "psp"
            "sony_psvita" -> "playstation-vita"
            "microsoft_xboxseriesx" -> "xbox-series-x"
            "microsoft_xboxone" -> "xbox-one"
            "microsoft_xbox360" -> "xbox-360"
            "microsoft_xbox" -> "xbox"
            "pc" -> "pc"
            else -> null
        }
    }
}
