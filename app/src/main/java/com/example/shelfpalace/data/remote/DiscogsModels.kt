package com.example.shelfpalace.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscogsPagination(
    val page: Int? = 1,
    val pages: Int? = 1,
    @param:Json(name = "per_page") @field:Json(name = "per_page") val perPage: Int? = 30,
    val items: Int? = 0
)

@JsonClass(generateAdapter = true)
data class DiscogsSearchResponse(
    val pagination: DiscogsPagination? = null,
    val results: List<DiscogsSearchResult>? = null
)

data class DiscogsSearchResultPage(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val results: List<DiscogsSearchResult>
)

@JsonClass(generateAdapter = true)
data class DiscogsMasterVersionsResponse(
    val versions: List<DiscogsMasterVersion>? = null
)

@JsonClass(generateAdapter = true)
data class DiscogsMasterVersion(
    val id: Long?,
    val title: String? = null,
    val format: String? = null,
    val country: String? = null,
    val released: String? = null,
    val label: String? = null,
    val catno: String? = null,
    val thumb: String? = null
) {
    fun toSearchResult(masterTitle: String, masterCover: String?): DiscogsSearchResult {
        val formatList = format?.split(",")?.map { it.trim() }
        val labelList = label?.let { listOf(it) }
        return DiscogsSearchResult(
            id = id,
            title = if (!title.isNullOrBlank()) title else masterTitle,
            year = released,
            country = country,
            format = formatList,
            label = labelList,
            thumb = thumb,
            coverImage = thumb ?: masterCover
        )
    }
}

@JsonClass(generateAdapter = true)
data class DiscogsSearchResult(
    val id: Long?,
    val title: String? = null,
    val year: String? = null,
    val country: String? = null,
    val format: List<String>? = null,
    val label: List<String>? = null,
    val genre: List<String>? = null,
    val style: List<String>? = null,
    @param:Json(name = "cover_image") @field:Json(name = "cover_image") val coverImage: String? = null,
    val thumb: String? = null,
    val barcode: List<String>? = null,
    @param:Json(name = "master_id") @field:Json(name = "master_id") val masterId: Long? = null
) {
    val validCoverUrl: String?
        get() {
            val url = when {
                !coverImage.isNullOrBlank() && !coverImage.contains("spacer.gif") -> coverImage
                !thumb.isNullOrBlank() && !thumb.contains("spacer.gif") -> thumb
                else -> null
            }
            return url?.let { if (it.startsWith("//")) "https:$it" else it }
        }

    val parsedArtist: String
        get() {
            val t = title ?: return ""
            return if (t.contains(" - ")) {
                t.substringBefore(" - ").trim()
            } else {
                ""
            }
        }

    val parsedTitle: String
        get() {
            val t = title ?: return ""
            return if (t.contains(" - ")) {
                t.substringAfter(" - ").trim()
            } else {
                t
            }
        }
}

@JsonClass(generateAdapter = true)
data class DiscogsRelease(
    val id: Long?,
    val title: String? = null,
    val artists: List<DiscogsArtist>? = null,
    val year: Long? = null,
    val released: String? = null,
    val genres: List<String>? = null,
    val styles: List<String>? = null,
    val labels: List<DiscogsLabel>? = null,
    val images: List<DiscogsImage>? = null,
    val tracklist: List<DiscogsTrack>? = null,
    val notes: String? = null,
    val identifiers: List<DiscogsIdentifier>? = null
)

@JsonClass(generateAdapter = true)
data class DiscogsArtist(
    val name: String? = null,
    val join: String? = null
)

@JsonClass(generateAdapter = true)
data class DiscogsLabel(
    val name: String? = null,
    val catno: String? = null
)

@JsonClass(generateAdapter = true)
data class DiscogsImage(
    val type: String? = null,
    val uri: String? = null,
    @param:Json(name = "resource_url") @field:Json(name = "resource_url") val resourceUrl: String? = null,
    val uri150: String? = null
)

@JsonClass(generateAdapter = true)
data class DiscogsTrack(
    val position: String? = null,
    val title: String? = null,
    val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class DiscogsIdentifier(
    val type: String? = null,
    val value: String? = null
)
