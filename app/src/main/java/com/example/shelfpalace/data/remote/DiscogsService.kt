package com.example.shelfpalace.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface DiscogsApi {
    @GET("database/search")
    suspend fun searchReleases(
        @Query("q") query: String? = null,
        @Query("artist") artist: String? = null,
        @Query("release_title") releaseTitle: String? = null,
        @Query("format") format: String? = null,
        @Query("label") label: String? = null,
        @Query("country") country: String? = null,
        @Query("year") year: String? = null,
        @Query("type") type: String = "release",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): DiscogsSearchResponse

    @GET("database/search")
    suspend fun searchByBarcode(
        @Query("barcode") barcode: String,
        @Query("type") type: String = "release"
    ): DiscogsSearchResponse

    @GET("releases/{id}")
    suspend fun getReleaseById(
        @Path("id") id: Long
    ): DiscogsRelease

    @GET("masters/{id}/versions")
    suspend fun getMasterVersions(
        @Path("id") masterId: Long,
        @Query("per_page") perPage: Int = 30
    ): DiscogsMasterVersionsResponse
}

object DiscogsService {
    private const val BASE_URL = "https://api.discogs.com/"
    
    // Discogs Personal User Token (optional, but avoids rate limits & enables native API images)
    var userToken: String = ""

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", "ShelfPalace/1.0 (Android App)")
            
            if (userToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Discogs token=$userToken")
            }
            
            chain.proceed(requestBuilder.build())
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(DiscogsApi::class.java)

    suspend fun search(
        query: String,
        format: String? = null,
        label: String? = null,
        country: String? = null,
        year: String? = null,
        page: Int = 1
    ): DiscogsSearchResultPage {
        val q = query.trim()
        val f = format?.trim()?.takeIf { it.isNotBlank() }
        val l = label?.trim()?.takeIf { it.isNotBlank() }
        val c = country?.trim()?.takeIf { it.isNotBlank() }
        val y = year?.trim()?.takeIf { it.isNotBlank() }

        if (q.isBlank() && f == null && l == null && c == null && y == null) {
            return DiscogsSearchResultPage(1, 1, 0, emptyList())
        }

        return try {
            if (q.all { it.isDigit() } && q.length >= 8 && f == null && l == null && c == null && y == null) {
                val barcodeResponse = api.searchByBarcode(q)
                val results = barcodeResponse.results ?: emptyList()
                if (results.isNotEmpty()) {
                    return DiscogsSearchResultPage(
                        currentPage = barcodeResponse.pagination?.page ?: 1,
                        totalPages = barcodeResponse.pagination?.pages ?: 1,
                        totalItems = barcodeResponse.pagination?.items ?: results.size,
                        results = results
                    )
                }
            }

            val cleanQuery = q.replace(" - ", " ").trim()

            fun mapPage(resp: DiscogsSearchResponse): DiscogsSearchResultPage? {
                val list = resp.results
                if (list.isNullOrEmpty()) return null
                return DiscogsSearchResultPage(
                    currentPage = resp.pagination?.page ?: page,
                    totalPages = resp.pagination?.pages ?: 1,
                    totalItems = resp.pagination?.items ?: list.size,
                    results = list
                )
            }

            var resp = api.searchReleases(
                query = cleanQuery.takeIf { it.isNotBlank() },
                format = f,
                label = l,
                country = c,
                year = y,
                page = page
            )
            mapPage(resp)?.let { return it }

            if (q.contains(" - ")) {
                val artistPart = q.substringBefore(" - ").trim().takeIf { it.isNotBlank() }
                val titlePart = q.substringAfter(" - ").trim().takeIf { it.isNotBlank() }
                if (artistPart != null && titlePart != null) {
                    resp = api.searchReleases(
                        artist = artistPart,
                        releaseTitle = titlePart,
                        format = f,
                        label = l,
                        country = c,
                        year = y,
                        page = page
                    )
                    mapPage(resp)?.let { return it }
                }
            }

            if (l != null || y != null) {
                resp = api.searchReleases(
                    query = cleanQuery.takeIf { it.isNotBlank() },
                    format = f,
                    country = c,
                    page = page
                )
                mapPage(resp)?.let { return it }
            }

            if (f != null) {
                resp = api.searchReleases(
                    query = cleanQuery.takeIf { it.isNotBlank() },
                    format = f,
                    page = page
                )
                mapPage(resp)?.let { return it }
            }

            if (cleanQuery.isNotBlank()) {
                resp = api.searchReleases(query = cleanQuery, page = page)
                mapPage(resp)?.let { return it }
            }

            DiscogsSearchResultPage(1, 1, 0, emptyList())
        } catch (e: Exception) {
            e.printStackTrace()
            DiscogsSearchResultPage(1, 1, 0, emptyList())
        }
    }

    suspend fun searchByBarcode(barcode: String): List<DiscogsSearchResult> {
        if (barcode.isBlank()) return emptyList()
        return try {
            val response = api.searchByBarcode(barcode = barcode.trim())
            response.results ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getReleaseById(id: Long): DiscogsRelease? {
        return try {
            api.getReleaseById(id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getMasterVersions(masterId: Long): List<DiscogsMasterVersion> {
        return try {
            val response = api.getMasterVersions(masterId)
            response.versions ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchReleaseCoverUrl(id: Long): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.discogs.com/release/$id")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36")
            conn.setRequestProperty("Referer", "https://www.discogs.com/")
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            
            val m1 = Regex("""property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
            val m2 = Regex("""content=["']([^"']+)["']\s+property=["']og:image["']""", RegexOption.IGNORE_CASE).find(html)
            val m3 = Regex("""<link\s+rel=["']image_src["']\s+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
            val m4 = Regex("""["']image["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)

            val found = m1?.groupValues?.get(1) ?: m2?.groupValues?.get(1) ?: m3?.groupValues?.get(1) ?: m4?.groupValues?.get(1)
            if (!found.isNullOrBlank() && !found.contains("spacer.gif")) {
                if (found.startsWith("//")) "https:$found" else found
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchAlbumCoverFallback(artist: String, title: String, releaseId: Long?): String? = withContext(Dispatchers.IO) {
        if (releaseId != null && releaseId > 0) {
            val scraped = fetchReleaseCoverUrl(releaseId)
            if (!scraped.isNullOrBlank() && !scraped.contains("spacer.gif")) {
                return@withContext scraped
            }
        }

        val searchTerms = listOfNotNull(
            "$artist $title".takeIf { artist.isNotBlank() && title.isNotBlank() },
            title.takeIf { it.isNotBlank() },
            artist.takeIf { it.isNotBlank() }
        )

        for (term in searchTerms) {
            try {
                val encodedTerm = URLEncoder.encode(term, "UTF-8")
                val url = URL("https://itunes.apple.com/search?term=$encodedTerm&entity=album&limit=1")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val match = Regex("""["']artworkUrl100["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(json)
                val artworkUrl = match?.groupValues?.get(1)
                if (!artworkUrl.isNullOrBlank()) {
                    val highResUrl = artworkUrl.replace("100x100bb.jpg", "600x600bb.jpg")
                    return@withContext highResUrl
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        null
    }
}
