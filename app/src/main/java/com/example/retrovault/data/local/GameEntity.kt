package com.example.retrovault.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.retrovault.data.Game

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val platformId: String,
    val title: String,
    val coverUri: String,
    val releaseDate: String,
    val genre: String,
    val developer: String,
    val description: String,
    val barcode: String? = null,
    val isFavorite: Boolean = false,
    val userRating: Double? = null,
    val criticRating: Double? = null,
    val igdbId: Long? = null,
    val dateAdded: Long,
    val status: String,
    val version: String,
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
    description = description,
    barcode = barcode,
    isFavorite = isFavorite,
    userRating = userRating,
    criticRating = criticRating,
    igdbId = igdbId,
    dateAdded = dateAdded,
    status = if (status.uppercase() == "BACKLOG") "Unplayed" else status,
    version = version,
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
    description = description,
    barcode = barcode,
    isFavorite = isFavorite,
    userRating = userRating,
    criticRating = criticRating,
    igdbId = igdbId,
    dateAdded = dateAdded,
    status = status,
    version = version,
    notes = notes
)
