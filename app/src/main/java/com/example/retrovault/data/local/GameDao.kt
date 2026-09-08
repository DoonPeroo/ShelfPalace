package com.example.retrovault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE platformId = :platformId")
    fun getGamesForPlatform(platformId: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%'")
    fun searchGames(query: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    fun getGameStream(id: String): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: String): GameEntity?

    @Query("SELECT * FROM games WHERE barcode = :barcode")
    suspend fun getGameByBarcode(barcode: String): GameEntity?

    @Query("SELECT * FROM games WHERE isFavorite = 1")
    fun getFavoriteGames(): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()
}
