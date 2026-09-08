package com.example.retrovault.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IgdbGame(
    val id: Long?,
    val name: String?,
    val summary: String? = null,
    val cover: IgdbCover? = null,
    @Json(name = "first_release_date") val firstReleaseDate: Long? = null,
    @Json(name = "involved_companies") val involvedCompanies: List<IgdbCompany>? = null,
    val genres: List<IgdbGenre>? = null,
    val rating: Double? = null,
    @Json(name = "aggregated_rating") val aggregatedRating: Double? = null,
    val platforms: List<Long>? = null,
    val screenshots: List<IgdbScreenshot>? = null
)

@JsonClass(generateAdapter = true)
data class IgdbScreenshot(
    val id: Long?,
    val url: String?
)

@JsonClass(generateAdapter = true)
data class IgdbCover(
    val id: Long?,
    val url: String?
)

@JsonClass(generateAdapter = true)
data class IgdbCompany(
    val id: Long?,
    val company: IgdbCompanyInfo?
)

@JsonClass(generateAdapter = true)
data class IgdbCompanyInfo(
    val id: Long?,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class IgdbGenre(
    val id: Long?,
    val name: String?
)
