package com.example.shelfpalace.data

import com.example.shelfpalace.data.local.MovieDao
import com.example.shelfpalace.data.local.toEntity
import com.example.shelfpalace.data.local.toExternalModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MovieRepository(private val movieDao: MovieDao) {
    fun getMoviesForFormat(formatId: String): Flow<List<Movie>> {
        val flow = if (formatId == "all") movieDao.getAllMovies() else movieDao.getMoviesForFormat(formatId)
        return flow.map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    fun searchMovies(query: String): Flow<List<Movie>> {
        return movieDao.searchMovies(query).map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    fun getAllMovies(): Flow<List<Movie>> {
        return movieDao.getAllMovies().map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    fun getMovieStream(id: String): Flow<Movie?> {
        return movieDao.getMovieStream(id).map { it?.toExternalModel() }
    }

    suspend fun getMovieById(id: String): Movie? {
        return movieDao.getMovieById(id)?.toExternalModel()
    }

    private fun isTitleDuplicate(existingTitle: String, enteredTitle: String): Boolean {
        val clean1 = existingTitle.lowercase().trim()
        val clean2 = enteredTitle.lowercase().trim()
        if (clean1 == clean2) return true
        val norm1 = clean1.replace("[^a-z0-9]".toRegex(), "")
        val norm2 = clean2.replace("[^a-z0-9]".toRegex(), "")
        return norm1.isNotEmpty() && norm1 == norm2
    }

    suspend fun doesMovieExist(title: String, formatId: String): Boolean {
        return doesMovieExistExcludingId(title, formatId, null)
    }

    suspend fun doesMovieExistExcludingId(title: String, formatId: String, excludeId: String?): Boolean {
        if (title.isBlank()) return false
        val list = if (formatId.isBlank() || formatId == "all") {
            movieDao.getAllMoviesList()
        } else {
            movieDao.getMoviesForFormatList(formatId)
        }
        return list.any { existing ->
            existing.id != excludeId && isTitleDuplicate(existing.title, title)
        }
    }

    suspend fun getMovieByBarcode(barcode: String): Movie? {
        return movieDao.getMovieByBarcode(barcode)?.toExternalModel()
    }

    fun getFavoriteMovies(): Flow<List<Movie>> {
        return movieDao.getFavoriteMovies().map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    suspend fun toggleFavorite(id: String) {
        movieDao.getMovieById(id)?.let {
            movieDao.updateMovie(it.copy(isFavorite = !it.isFavorite))
        }
    }

    suspend fun insertMovie(movie: Movie) {
        movieDao.insertMovie(movie.toEntity())
    }

    suspend fun insertMovies(movies: List<Movie>) {
        movieDao.insertMovies(movies.map { it.toEntity() })
    }

    suspend fun updateMovie(movie: Movie) {
        movieDao.insertMovie(movie.toEntity())
    }

    suspend fun deleteMovie(movie: Movie) {
        movieDao.deleteMovie(movie.toEntity())
    }

    suspend fun clearAllMovies() {
        movieDao.deleteAllMovies()
    }
}
