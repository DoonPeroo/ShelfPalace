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

    suspend fun doesMovieExist(title: String, formatId: String): Boolean {
        return movieDao.getMovieByTitleAndFormat(title, formatId) != null
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
        val movie = movieDao.getMovieById(id)
        if (movie != null) {
            movieDao.updateMovie(movie.copy(isFavorite = !movie.isFavorite))
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
