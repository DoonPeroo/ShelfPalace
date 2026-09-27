package com.example.shelfpalace.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
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
        @Body query: RequestBody,
    ): List<IgdbGame>

    @POST("external_games")
    suspend fun searchExternalGames(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authorization: String,
        @Body query: RequestBody,
    ): List<IgdbExternalGame>
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

    private const val COMMON_FIELDS = "name, summary, cover.url, first_release_date, genres.name, involved_companies.company.name, rating, aggregated_rating, total_rating, platforms, screenshots.url, videos.video_id, videos.name"

    private val GERMAN_TO_ENGLISH_TITLES = mapOf(
        "der herr der ringe" to "the lord of the rings",
        "herr der ringe" to "lord of the rings",
        "die eroberung" to "the lord of the rings: conquest",
        "eroberung" to "the lord of the rings: conquest",
        "conquest" to "the lord of the rings: conquest",
        "schlacht um mittelerde" to "the lord of the rings: battle for middle earth",
        "battle for middle earth" to "the lord of the rings: battle for middle earth",
        "das dritte zeitalter" to "the lord of the rings: the third age",
        "the third age" to "the lord of the rings: the third age",
        "die rückkehr des königs" to "the lord of the rings: return of the king",
        "die rueckkehr des koenigs" to "the lord of the rings: return of the king",
        "return of the king" to "the lord of the rings: return of the king",
        "die zwei türme" to "the lord of the rings: the two towers",
        "die zwei tuerme" to "the lord of the rings: the two towers",
        "the two towers" to "the lord of the rings: the two towers",
        "die gefährten" to "the lord of the rings: fellowship of the ring",
        "die gefaehrten" to "the lord of the rings: fellowship of the ring",
        "fellowship of the ring" to "the lord of the rings: fellowship of the ring",
        "mordors schatten" to "middle earth shadow of mordor",
        "schatten des krieges" to "middle earth shadow of war",
        "der krieg im norden" to "the lord of the rings war in the north",
        "krieg der sterne" to "star wars",
        "fluch der karibik" to "pirates of the caribbean",
        "die hüter des lichts" to "rise of the guardians",
        "hueter des lichts" to "rise of the guardians",
        "moorhuhn" to "crazy chicken",
        "schlag den raab" to "beat the raab",
        "landwirtschafts simulator" to "farming simulator",
        "landwirtschafts-simulator" to "farming simulator",
        "feuerwehr simulator" to "firefighting simulator",
        "bau simulator" to "construction simulator",
        "die siedler" to "the settlers",
        "die schlümpfe" to "the smurfs",
        "die schlumpfe" to "the smurfs",
        "wickie und die starken männer" to "vicky the viking",
        "die biene maja" to "maya the bee",
        "die simpsons" to "the simpsons",
        "drachenzähmen leicht gemacht" to "how to train your dragon",
        "ich einfach unverbesserlich" to "despicable me",
        "zoomania" to "zootopia",
        "rapunzel neu verföhnt" to "tangled",
        "die eiskönigin" to "frozen",
        "alles steht kopf" to "inside out",
        "findet nemo" to "finding nemo",
        "findet dorie" to "finding dory",
        "die unglaublichen" to "the incredibles",
        "glücksbärchis" to "care bears",
        "der magische stift" to "drawn to life",
        "magische stift" to "drawn to life",
        "das geheimnisvolle dorf" to "curious village",
        "die schatulle der panik" to "diabolical box",
        "die verlorene zukunft" to "unwound future",
        "zusammen durch die zeit" to "partners in time",
        "yogi bär" to "yogi bear",
        "yogi baer" to "yogi bear",
        "spongebob schwammkopf" to "spongebob squarepants",
        "winnie puuh" to "winnie the pooh",
        "das videospiel" to "",
        "das spiel zum film" to "",
        "meine tierarztpraxis" to "pet vet",
        "meine fohlenwelt" to "my horse park",
    )

    suspend fun search(title: String, platformId: String? = null): List<IgdbGame> {
        if (clientId.isEmpty() || accessToken.isEmpty()) return emptyList()
        if (title.isBlank()) return emptyList()
        
        val trimmedTitle = title.trim()
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

        // Build list of search queries placing the English translated query FIRST
        val searchQueries = mutableListOf<String>()
        val cleanLower = trimmedTitle.lowercase()

        var fullyTranslated = cleanLower
        GERMAN_TO_ENGLISH_TITLES.entries.sortedByDescending { it.key.length }.forEach { (german, english) ->
            val regex = Regex("(?i)\\b${Regex.escape(german)}\\b")
            if (regex.containsMatchIn(fullyTranslated)) {
                fullyTranslated = fullyTranslated.replace(regex, english)
            }
        }

        if (fullyTranslated != cleanLower && fullyTranslated.isNotBlank()) {
            searchQueries.add(fullyTranslated)
        }
        searchQueries.add(trimmedTitle)

        if (cleanLower.contains("crazy chicken")) {
            searchQueries.add(cleanLower.replace("crazy chicken", "moorhuhn"))
        } else if (cleanLower.contains("moorhuhn")) {
            searchQueries.add(cleanLower.replace("moorhuhn", "crazy chicken"))
        }

        val candidateGames = mutableListOf<IgdbGame>()

        for (rawQueryText in searchQueries) {
            val queryText = rawQueryText.replace(Regex("[:\\-_/()|]"), " ").replace(Regex("\\s+"), " ").trim()
            val escaped = queryText.replace("\"", "\\\"")
            if (escaped.isBlank()) continue

            // Stage 1: Search with platform filter
            if (igdbPlatformId != null) {
                val res1 = executeQuery("search \"$escaped\"; fields $COMMON_FIELDS; where platforms = ($igdbPlatformId); limit 100;")
                candidateGames.addAll(res1)
            }

            // Stage 2: Global search
            if (candidateGames.isEmpty()) {
                val res2 = executeQuery("search \"$escaped\"; fields $COMMON_FIELDS; limit 100;")
                candidateGames.addAll(res2)
            }

            // Stage 3: Search by alternative_names
            if (candidateGames.isEmpty()) {
                val resAlt = executeQuery("fields $COMMON_FIELDS; where alternative_names.name ~ *\"$escaped\"*; limit 100;")
                candidateGames.addAll(resAlt)
            }

            // Stage 4: Tokenized wildcard search for multi-word queries
            if (candidateGames.isEmpty()) {
                val words = queryText.split("\\s+".toRegex()).filter { it.length > 1 }
                if (words.size > 1) {
                    val tokenFilter = words.joinToString(" & ") { word ->
                        "name ~ *\"${word.replace("\"", "\\\"")}\"*"
                    }
                    val platformClause = if (igdbPlatformId != null) " & platforms = ($igdbPlatformId)" else ""
                    val res3 = executeQuery("fields $COMMON_FIELDS; where $tokenFilter$platformClause; limit 100;")
                    candidateGames.addAll(res3)
                }
            }
        }

        if (candidateGames.isNotEmpty()) {
            val ranked = candidateGames.distinctBy { it.id }
                .sortedByDescending { scoreIgdbResult(it, trimmedTitle) }

            if (ranked.isNotEmpty() && scoreIgdbResult(ranked.first(), trimmedTitle) >= 1.5) {
                return ranked
            }
        }

        return emptyList()
    }

    private fun scoreIgdbResult(game: IgdbGame, searchTitle: String): Double {
        var cleanSearch = searchTitle.lowercase()
        GERMAN_TO_ENGLISH_TITLES.entries.sortedByDescending { it.key.length }.forEach { (german, english) ->
            val regex = Regex("(?i)\\b${Regex.escape(german)}\\b")
            if (regex.containsMatchIn(cleanSearch)) {
                cleanSearch = cleanSearch.replace(regex, english)
            }
        }
        cleanSearch = cleanSearch
            .replace("crazy chicken", "moorhuhn")
            .replace(Regex("[^a-z0-9]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        var cleanName = (game.name ?: "").lowercase()
        GERMAN_TO_ENGLISH_TITLES.entries.sortedByDescending { it.key.length }.forEach { (german, english) ->
            val regex = Regex("(?i)\\b${Regex.escape(german)}\\b")
            if (regex.containsMatchIn(cleanName)) {
                cleanName = cleanName.replace(regex, english)
            }
        }
        cleanName = cleanName
            .replace("crazy chicken", "moorhuhn")
            .replace(Regex("[^a-z0-9]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanSearch.isEmpty() || cleanName.isEmpty()) return 0.0
        if (cleanSearch == cleanName) return 15.0

        val searchWords = cleanSearch.split(" ").filter { it.length >= 2 }.toSet()
        val nameWords = cleanName.split(" ").filter { it.length >= 2 }.toSet()

        if (searchWords.isEmpty() || nameWords.isEmpty()) return 0.0

        val common = searchWords.intersect(nameWords)
        if (common.isEmpty()) return 0.0

        val searchRatio = common.size.toDouble() / searchWords.size
        val nameRatio = common.size.toDouble() / nameWords.size

        // Candidate game MUST match all search words to qualify for full match bonus
        val matchesAllSearchWords = common.size == searchWords.size

        if (matchesAllSearchWords) {
            return 10.0 + nameRatio
        }

        return searchRatio * 5.0 + nameRatio * 2.0
    }

    suspend fun searchByBarcode(barcode: String): IgdbGame? {
        if (clientId.isEmpty() || accessToken.isEmpty()) return null
        if (barcode.isBlank()) return null

        val clean = barcode.trim().filter { it.isDigit() }
        val stripped = clean.trimStart('0')
        val variations = listOf(clean, stripped, stripped.padStart(13, '0'), stripped.padStart(12, '0')).distinct()

        val uidFilter = variations.joinToString(" | ") { "uid = \"$it\"" }
        val bodyString = "fields game.$COMMON_FIELDS; where $uidFilter; limit 10;"

        return try {
            val body = bodyString.toRequestBody("text/plain".toMediaType())
            val externalResults = api.searchExternalGames(clientId, "Bearer $accessToken", body)
            externalResults.firstOrNull { it.game != null }?.game
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getGameById(id: Long): IgdbGame? {
        if (clientId.isEmpty() || accessToken.isEmpty()) return null
        
        val bodyString = "fields $COMMON_FIELDS; where id = $id;"
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
