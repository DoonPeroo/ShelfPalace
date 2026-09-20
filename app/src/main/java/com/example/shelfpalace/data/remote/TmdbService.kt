package com.example.shelfpalace.data.remote

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class FlexibleDoubleAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Double? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> reader.nextNull()
            JsonReader.Token.NUMBER -> reader.nextDouble()
            JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Double?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value)
        }
    }
}

interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("primary_release_year") primaryReleaseYear: String? = null,
        @Query("year") year: String? = null,
        @Query("language") language: String = "de-DE",
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits,images",
        @Query("language") language: String = "de-DE"
    ): TmdbMovieDetails

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "de-DE"
    ): TmdbVideosResponse

    @GET("movie/{movie_id}/images")
    suspend fun getMovieImages(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String
    ): TmdbImagesResponse
}

object TmdbService {
    private const val BASE_URL = "https://api.themoviedb.org/3/"
    
    val TMDB_API_KEYS = listOf(
        "15d2ea6d0dc1d476efbca3ecc2b92f23",
        "c36d393de5c510166a93e36e65a12708",
        "38a73d59546aa378980a88b645f487fc",
        "f1738221841315f606399b1a13e51240"
    )

    var apiKey: String = TMDB_API_KEYS.first()

    private val moshi = Moshi.Builder()
        .add(FlexibleDoubleAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", "ShelfPalace/1.0 (Android App)")
                .header("Accept", "application/json")
            chain.proceed(requestBuilder.build())
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(TmdbApi::class.java)

    suspend fun search(
        query: String,
        year: String? = null,
        page: Int = 1,
        language: String = "en-US"
    ): TmdbSearchResultPage {
        val q = query.trim()
        val y = year?.trim()?.takeIf { it.isNotBlank() }

        if (q.isBlank()) {
            return TmdbSearchResultPage(1, 1, 0, emptyList())
        }

        val keysToTry = (listOf(apiKey) + TMDB_API_KEYS).distinct()

        for (key in keysToTry) {
            try {
                val resp1 = api.searchMovies(
                    query = q,
                    apiKey = key,
                    page = page,
                    primaryReleaseYear = y,
                    language = language
                )
                val list1 = resp1.results
                if (!list1.isNullOrEmpty()) {
                    return TmdbSearchResultPage(
                        currentPage = resp1.page ?: page,
                        totalPages = resp1.totalPages ?: 1,
                        totalResults = resp1.totalResults ?: list1.size,
                        results = list1
                    )
                }

                if (y != null) {
                    val respNoYear = api.searchMovies(
                        query = q,
                        apiKey = key,
                        page = page,
                        language = language
                    )
                    val listNoYear = respNoYear.results
                    if (!listNoYear.isNullOrEmpty()) {
                        return TmdbSearchResultPage(
                            currentPage = respNoYear.page ?: page,
                            totalPages = respNoYear.totalPages ?: 1,
                            totalResults = respNoYear.totalResults ?: listNoYear.size,
                            results = listNoYear
                        )
                    }
                }

                val respEn = api.searchMovies(
                    query = q,
                    apiKey = key,
                    page = page,
                    primaryReleaseYear = y,
                    language = "en-US"
                )
                val listEn = respEn.results
                if (!listEn.isNullOrEmpty()) {
                    return TmdbSearchResultPage(
                        currentPage = respEn.page ?: page,
                        totalPages = respEn.totalPages ?: 1,
                        totalResults = respEn.totalResults ?: listEn.size,
                        results = listEn
                    )
                }

                if (y != null) {
                    val respEnNoYear = api.searchMovies(
                        query = q,
                        apiKey = key,
                        page = page,
                        language = "en-US"
                    )
                    val listEnNoYear = respEnNoYear.results
                    if (!listEnNoYear.isNullOrEmpty()) {
                        return TmdbSearchResultPage(
                            currentPage = respEnNoYear.page ?: page,
                            totalPages = respEnNoYear.totalPages ?: 1,
                            totalResults = respEnNoYear.totalResults ?: listEnNoYear.size,
                            results = listEnNoYear
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return TmdbSearchResultPage(1, 1, 0, emptyList())
    }

    suspend fun getMovieDetails(movieId: Long, language: String = "en-US"): TmdbMovieDetails? {
        val keysToTry = (listOf(apiKey) + TMDB_API_KEYS).distinct()
        for (key in keysToTry) {
            try {
                val details = api.getMovieDetails(movieId = movieId, apiKey = key, language = language)
                if (details != null) {
                    if ((details.overview.isNullOrBlank() || details.genres.isNullOrEmpty()) && language != "en-US") {
                        try {
                            val enDetails = api.getMovieDetails(movieId = movieId, apiKey = key, language = "en-US")
                            return details.copy(
                                overview = details.overview.takeIf { !it.isNullOrBlank() } ?: enDetails.overview,
                                genres = details.genres.takeIf { !it.isNullOrEmpty() } ?: enDetails.genres
                            )
                        } catch (_: Exception) {}
                    }
                    return details
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    return api.getMovieDetails(movieId = movieId, apiKey = key, language = "en-US")
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
        return null
    }

    suspend fun fetchMovieMedia(movieTitle: String, releaseYear: String = "", language: String = "en-US"): Pair<List<TmdbImage>, List<TmdbVideo>> {
        val searchResult = search(query = movieTitle, year = releaseYear, language = language)
        val firstMovieId = searchResult.results.firstOrNull()?.id ?: return Pair(emptyList(), emptyList())
        return fetchMovieMediaById(firstMovieId, language = language)
    }

    suspend fun fetchMovieMediaById(movieId: Long, language: String = "en-US"): Pair<List<TmdbImage>, List<TmdbVideo>> {
        val keysToTry = (listOf(apiKey) + TMDB_API_KEYS).distinct()
        var images = emptyList<TmdbImage>()
        var videos = emptyList<TmdbVideo>()

        for (key in keysToTry) {
            try {
                if (videos.isEmpty()) {
                    val vRespLang = api.getMovieVideos(movieId, key, language)
                    videos = vRespLang.results?.filter { it.site.equals("YouTube", ignoreCase = true) && !it.key.isNullOrBlank() } ?: emptyList()

                    if (videos.isEmpty() && language != "en-US") {
                        val vRespEn = api.getMovieVideos(movieId, key, "en-US")
                        videos = vRespEn.results?.filter { it.site.equals("YouTube", ignoreCase = true) && !it.key.isNullOrBlank() } ?: emptyList()
                    }

                    if (videos.isEmpty() && language != "de-DE" && language != "en-US") {
                        val vRespDe = api.getMovieVideos(movieId, key, "de-DE")
                        videos = vRespDe.results?.filter { it.site.equals("YouTube", ignoreCase = true) && !it.key.isNullOrBlank() } ?: emptyList()
                    }
                }
                if (images.isEmpty()) {
                    val iResp = api.getMovieImages(movieId, key)
                    images = iResp.backdrops ?: emptyList()
                }
                if (images.isNotEmpty() || videos.isNotEmpty()) break
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Pair(images, videos)
    }
}
