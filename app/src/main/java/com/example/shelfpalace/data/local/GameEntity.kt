package com.example.shelfpalace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shelfpalace.data.Game

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val platformId: String,
    val title: String,
    val coverUri: String,
    val releaseDate: String,
    val genre: String,
    val developer: String,
    val publisher: String = "",
    val description: String,
    val barcode: String? = null,
    val isFavorite: Boolean = false,
    val userRating: Double? = null,
    val criticRating: Double? = null,
    val igdbId: Long? = null,
    val dateAdded: Long,
    val status: String,
    val condition: String = "Sealed",
    val gameEdition: String = "Retail",
    val notes: String
)

fun GameEntity.toExternalModel() = Game(
    id = id,
    platformId = platformId,
    title = title,
    coverUri = coverUri,
    releaseDate = releaseDate,
    genre = genre,
    developer = developer,
    publisher = publisher,
    description = description,
    barcode = barcode,
    isFavorite = isFavorite,
    userRating = userRating,
    criticRating = criticRating,
    igdbId = igdbId,
    dateAdded = dateAdded,
    status = if (status.uppercase() == "BACKLOG") "Unplayed" else status,
    condition = condition,
    gameEdition = gameEdition,
    notes = notes
)

fun Game.toEntity() = GameEntity(
    id = id,
    platformId = platformId,
    title = title,
    coverUri = coverUri,
    releaseDate = releaseDate,
    genre = genre,
    developer = developer,
    publisher = publisher,
    description = description,
    barcode = barcode,
    isFavorite = isFavorite,
    userRating = userRating,
    criticRating = criticRating,
    igdbId = igdbId,
    dateAdded = dateAdded,
    status = status,
    condition = condition,
    gameEdition = gameEdition,
    notes = notes
)
