package com.example.shelfpalace.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface IgdbApi {
    @POST("games")
    suspend fun searchGames(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authorization: String,
        @Body query: okhttp3.RequestBody
    ): List<IgdbGame>
}

object IgdbService {
    private const val BASE_URL = "https://api.igdb.com/v4/"
    
    // NOTE: You must provide your own IGDB Client ID and Access Token.
    // Get them at https://api-docs.igdb.com/
    var clientId: String = "d5fqewta9lmzzjo63blpbi4277wpea"
    var accessToken: String = "w8krvz776nbp3kxzbq9unqwgewuym5"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(IgdbApi::class.java)

    private val commonFields = "name, summary, cover.url, first_release_date, genres.name, involved_companies.company.name, rating, aggregated_rating, platforms, screenshots.url"

    suspend fun search(title: String, platformId: String? = null): List<IgdbGame> {
        if (clientId.isEmpty() || accessToken.isEmpty()) return emptyList()
        if (title.isBlank()) return emptyList()
        
        val trimmedTitle = title.trim()
        val escapedTitle = trimmedTitle.replace("\"", "\\\"")
        val igdbPlatformId = platformId?.takeIf { it != "all" }?.let { getIgdbPlatformId(it) }
        
        suspend fun executeQuery(bodyString: String): List<IgdbGame> {
            return try {
                val body = bodyString.toRequestBody("text/plain".toMediaType())
                api.searchGames(clientId, "Bearer $accessToken", body)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

        // Stage 1: Try exact search with platform filter
        if (igdbPlatformId != null) {
            val res1 = executeQuery("search \"$escapedTitle\"; fields $commonFields; where platforms = ($igdbPlatformId); limit 50;")
            if (res1.isNotEmpty()) return res1
        }
        
        // Stage 2: Try global exact search without platform restriction
        val res2 = executeQuery("search \"$escapedTitle\"; fields $commonFields; limit 50;")
        if (res2.isNotEmpty()) return res2

        // Stage 3: Tokenized wildcard search for multi-word queries (e.g. "Mario Wonder" -> name ~ *"Mario"* & name ~ *"Wonder"*)
        val words = trimmedTitle.split("\\s+".toRegex()).filter { it.length > 1 }
        if (words.size > 1) {
            val tokenFilter = words.joinToString(" & ") { word ->
                "name ~ *\"${word.replace("\"", "\\\"")}\"*"
            }
            val platformClause = if (igdbPlatformId != null) " & platforms = ($igdbPlatformId)" else ""
            val res3 = executeQuery("fields $commonFields; where $tokenFilter$platformClause; limit 50;")
            if (res3.isNotEmpty()) return res3
            
            if (igdbPlatformId != null) {
                val res4 = executeQuery("fields $commonFields; where $tokenFilter; limit 50;")
                if (res4.isNotEmpty()) return res4
            }
        }

        // Stage 4: Try searching with the longest single word
        val longestWord = words.maxByOrNull { it.length }
        if (longestWord != null && longestWord.length >= 3) {
            val res5 = executeQuery("search \"${longestWord.replace("\"", "\\\"")}\"; fields $commonFields; limit 50;")
            if (res5.isNotEmpty()) return res5
        }

        return emptyList()
    }

    suspend fun getGameById(id: Long): IgdbGame? {
        if (clientId.isEmpty() || accessToken.isEmpty()) return null
        
        val bodyString = "fields $commonFields; where id = $id;"
        val body = bodyString.toRequestBody("text/plain".toMediaType())
        
        return try {
            api.searchGames(clientId, "Bearer $accessToken", body).firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getIgdbPlatformId(platformId: String): Int? {
        return when (platformId) {
            "sony_ps1" -> 7
            "sony_ps2" -> 8
            "sony_ps3" -> 9
            "sony_ps4" -> 48
            "sony_ps5" -> 167
            "sony_psp" -> 38
            "sony_psvita" -> 46
            "nintendo_nes" -> 18
            "nintendo_snes" -> 19
            "nintendo_n64" -> 4
            "nintendo_gamecube" -> 21
            "nintendo_wii" -> 5
            "nintendo_wiiu" -> 41
            "nintendo_switch" -> 130
            "nintendo_switch2" -> null
            "nintendo_gb" -> 33
            "nintendo_gbc" -> 24
            "nintendo_gba" -> 22
            "nintendo_ds" -> 20
            "nintendo_3ds" -> 37
            "microsoft_xbox" -> 11
            "microsoft_xbox360" -> 12
            "microsoft_xboxone" -> 49
            "microsoft_xboxseriesx" -> 169
            "sega_ms" -> 64
            "sega_md" -> 29
            "sega_saturn" -> 32
            "sega_dreamcast" -> 23
            "sega_gg" -> 35
            else -> null
        }
    }
}
