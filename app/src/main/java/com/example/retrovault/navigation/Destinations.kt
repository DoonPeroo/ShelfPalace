package com.example.retrovault.navigation

import kotlinx.serialization.Serializable

sealed interface Destinations {
    @Serializable
    data object LibraryDashboard : Destinations

    @Serializable
    data object ManufacturerList : Destinations
    
    @Serializable
    data object MovieFormatList : Destinations

    @Serializable
    data object MusicFormatList : Destinations

    @Serializable
    data object MediaSelection : Destinations

    @Serializable
    data class PlatformList(val manufacturerId: String) : Destinations
    
    @Serializable
    data class GameList(val platformId: String) : Destinations

    @Serializable
    data class GameDetail(val gameId: String) : Destinations

    @Serializable
    data class AddEditGame(val platformId: String? = null, val gameId: String? = null, val igdbId: Long? = null) : Destinations

    @Serializable
    data class MovieList(val formatId: String) : Destinations

    @Serializable
    data class MovieDetail(val movieId: String) : Destinations

    @Serializable
    data class AddEditMovie(val formatId: String? = null, val movieId: String? = null) : Destinations

    @Serializable
    data class MusicList(val formatId: String) : Destinations

    @Serializable
    data class MusicDetail(val musicId: String) : Destinations

    @Serializable
    data class AddEditMusic(val formatId: String? = null, val musicId: String? = null) : Destinations

    @Serializable
    data object Scanner : Destinations

    @Serializable
    data object Search : Destinations

    @Serializable
    data object Settings : Destinations

    @Serializable
    data object Favorites : Destinations

    @Serializable
    data object Statistics : Destinations

    @Serializable
    data class IgdbSearch(val platformId: String? = null, val gameId: String? = null, val initialQuery: String? = null) : Destinations
}
