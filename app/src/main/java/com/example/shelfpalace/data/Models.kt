package com.example.shelfpalace.data

import kotlinx.serialization.Serializable

@Serializable
data class Manufacturer(
    val id: String,
    val name: String,
    val logo: String // URL or Resource name
)

@Serializable
data class Platform(
    val id: String,
    val manufacturerId: String,
    val name: String
)

@Serializable
data class Game(
    val id: String,
    val platformId: String,
    val title: String,
    val coverUri: String,
    val releaseDate: String,
    val genre: String = "",
    val developer: String = "",
    val publisher: String = "",
    val description: String,
    val barcode: String? = null,
    val isFavorite: Boolean = false,
    val userRating: Double? = null,
    val criticRating: Double? = null,
    val igdbId: Long? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val status: String = "Unplayed",
    val condition: String = "Sealed",
    val gameEdition: String = "Retail",
    val purchaseDate: String = "",
    val pricePaid: String = "",
    val notes: String = ""
)

@Serializable
data class Movie(
    val id: String,
    val formatId: String, // VHS, Blu-ray, etc.
    val title: String,
    val coverUri: String,
    val releaseDate: String,
    val genre: String = "",
    val director: String = "",
    val cast: String = "",
    val description: String,
    val barcode: String? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val status: String = "Plan to watch",
    val purchaseDate: String = "",
    val pricePaid: String = "",
    val notes: String = ""
)

@Serializable
data class MovieFormat(
    val id: String,
    val name: String,
    val iconName: String = "" // Added for format icons
)

@Serializable
data class Music(
    val id: String,
    val formatId: String, // CD, Cassette, Vinyl
    val title: String,
    val artist: String,
    val coverUri: String,
    val releaseDate: String,
    val genre: String = "",
    val description: String = "",
    val label: String = "",
    val barcode: String? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val status: String = "Plan to listen",
    val purchaseDate: String = "",
    val pricePaid: String = "",
    val notes: String = ""
)

@Serializable
data class MusicFormat(
    val id: String,
    val name: String,
    val iconName: String = ""
)

enum class SortOption {
    NAME,
    RELEASE_DATE,
    PLATFORM
}

enum class CornerStyle {
    ROUNDED,
    SQUARE
}

@Serializable
data class BackupData(
    val games: List<Game> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val music: List<Music> = emptyList(),
    val disabledIds: List<String> = emptyList(),
    val sortOption: String = "NAME",
    val dashboardFilter: String = "Recently Added",
    val cornerStyle: String = "ROUNDED"
)
