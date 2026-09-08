package com.example.retrovault.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.retrovault.data.Music

@Entity(tableName = "music")
data class MusicEntity(
    @PrimaryKey val id: String,
    val formatId: String,
    val title: String,
    val artist: String,
    val coverUri: String,
    val releaseDate: String,
    val genre: String,
    val description: String,
    val label: String,
    val barcode: String? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long,
    val status: String,
    val version: String,
    val notes: String
)

fun MusicEntity.toExternalModel() = Music(
    id = id,
    formatId = formatId,
    title = title,
    artist = artist,
    coverUri = coverUri,
    releaseDate = releaseDate,
    genre = genre,
    description = description,
    label = label,
    barcode = barcode,
    isFavorite = isFavorite,
    dateAdded = dateAdded,
    status = status,
    version = version,
    notes = notes
)

fun Music.toEntity() = MusicEntity(
    id = id,
    formatId = formatId,
    title = title,
    artist = artist,
    coverUri = coverUri,
    releaseDate = releaseDate,
    genre = genre,
    description = description,
    label = label,
    barcode = barcode,
    isFavorite = isFavorite,
    dateAdded = dateAdded,
    status = status,
    version = version,
    notes = notes
)
