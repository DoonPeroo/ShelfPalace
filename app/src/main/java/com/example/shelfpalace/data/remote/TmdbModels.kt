package com.example.shelfpalace.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    val page: Int? = 1,
    val results: List<TmdbMovie>? = null,
    @param:Json(name = "total_pages") @field:Json(name = "total_pages") val totalPages: Int? = 1,
    @param:Json(name = "total_results") @field:Json(name = "total_results") val totalResults: Int? = 0
)

data class TmdbSearchResultPage(
    val currentPage: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<TmdbMovie>
)

@JsonClass(generateAdapter = true)
data class TmdbMovie(
    val id: Long?,
    val title: String? = null,
    @param:Json(name = "original_title") @field:Json(name = "original_title") val originalTitle: String? = null,
    val overview: String? = null,
    @param:Json(name = "poster_path") @field:Json(name = "poster_path") val posterPath: String? = null,
    @param:Json(name = "backdrop_path") @field:Json(name = "backdrop_path") val backdropPath: String? = null,
    @param:Json(name = "release_date") @field:Json(name = "release_date") val releaseDate: String? = null,
    @param:Json(name = "genre_ids") @field:Json(name = "genre_ids") val genreIds: List<Int>? = null,
    @param:Json(name = "vote_average") @field:Json(name = "vote_average") val voteAverage: Double? = null
) {
    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val releaseYear: String
        get() = releaseDate?.take(4) ?: ""
}

@JsonClass(generateAdapter = true)
data class TmdbProductionCompany(
    val id: Long? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbMovieDetails(
    val id: Long?,
    val title: String? = null,
    @param:Json(name = "original_title") @field:Json(name = "original_title") val originalTitle: String? = null,
    val overview: String? = null,
    @param:Json(name = "poster_path") @field:Json(name = "poster_path") val posterPath: String? = null,
    @param:Json(name = "backdrop_path") @field:Json(name = "backdrop_path") val backdropPath: String? = null,
    @param:Json(name = "release_date") @field:Json(name = "release_date") val releaseDate: String? = null,
    val genres: List<TmdbGenre>? = null,
    val credits: TmdbCredits? = null,
    @param:Json(name = "production_companies") @field:Json(name = "production_companies") val productionCompanies: List<TmdbProductionCompany>? = null,
    val runtime: Int? = null,
    @param:Json(name = "vote_average") @field:Json(name = "vote_average") val voteAverage: Double? = null
) {
    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val releaseYear: String
        get() = releaseDate?.take(4) ?: ""

    val directorName: String
        get() = credits?.crew?.find { it.job.equals("Director", ignoreCase = true) }?.name ?: ""

    val castNames: String
        get() = credits?.cast?.take(5)?.joinToString(", ") { it.name ?: "" } ?: ""

    val genreNames: String
        get() = genres?.joinToString(", ") { it.name ?: "" } ?: ""

    val studioName: String
        get() = productionCompanies?.take(2)?.mapNotNull { it.name }?.filter { it.isNotBlank() }?.joinToString(", ") ?: ""
}

@JsonClass(generateAdapter = true)
data class TmdbGenre(
    val id: Int?,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCredits(
    val cast: List<TmdbCast>? = null,
    val crew: List<TmdbCrew>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCast(
    val id: Long? = null,
    val name: String? = null,
    val character: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCrew(
    val id: Long? = null,
    val name: String? = null,
    val job: String? = null,
    val department: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbVideosResponse(
    val id: Long? = null,
    val results: List<TmdbVideo>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbVideo(
    val id: String? = null,
    val name: String? = null,
    val key: String? = null,
    val site: String? = null,
    val type: String? = null,
    val official: Boolean? = false
) {
    val youtubeUrl: String
        get() = "https://www.youtube.com/watch?v=$key"

    val thumbnailUrl: String
        get() = "https://img.youtube.com/vi/$key/hqdefault.jpg"
}

@JsonClass(generateAdapter = true)
data class TmdbImagesResponse(
    val id: Long? = null,
    val backdrops: List<TmdbImage>? = null,
    val posters: List<TmdbImage>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbImage(
    @param:Json(name = "file_path") @field:Json(name = "file_path") val filePath: String? = null,
    val width: Int? = null,
    val height: Int? = null
) {
    val fullUrl: String?
        get() = filePath?.let { "https://image.tmdb.org/t/p/w780$it" }
}
