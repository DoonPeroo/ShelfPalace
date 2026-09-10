package com.example.shelfpalace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shelfpalace.data.Movie

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val formatId: String,
    val title: String,
    val coverUri: String,
    val releaseDate: String,
    val genre: String,
    val director: String,
    val cast: String,
    val description: String,
    val barcode: String? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long,
    val status: String,
    val purchaseDate: String = "",
    val pricePaid: String = "",
    val notes: String
)

fun MovieEntity.toExternalModel() = Movie(
    id = id,
    formatId = formatId,
    title = title,
    coverUri = coverUri,
    releaseDate = releaseDate,
    genre = genre,
    director = director,
    cast = cast,
    description = description,
    barcode = barcode,
    isFavorite = isFavorite,
    dateAdded = dateAdded,
    status = status,
    purchaseDate = purchaseDate,
    pricePaid = pricePaid,
    notes = notes
)

fun Movie.toEntity() = MovieEntity(
    id = id,
    formatId = formatId,
    title = title,
    coverUri = coverUri,
    releaseDate = releaseDate,
    genre = genre,
    director = director,
    cast = cast,
    description = description,
    barcode = barcode,
    isFavorite = isFavorite,
    dateAdded = dateAdded,
    status = status,
    purchaseDate = purchaseDate,
    pricePaid = pricePaid,
    notes = notes
)
