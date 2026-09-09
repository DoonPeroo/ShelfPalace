package com.example.shelfpalace.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM music WHERE formatId = :formatId")
    fun getMusicForFormat(formatId: String): Flow<List<MusicEntity>>

    @Query("SELECT * FROM music WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchMusic(query: String): Flow<List<MusicEntity>>

    @Query("SELECT * FROM music")
    fun getAllMusic(): Flow<List<MusicEntity>>

    @Query("SELECT * FROM music WHERE id = :id")
    fun getMusicStream(id: String): Flow<MusicEntity?>

    @Query("SELECT * FROM music WHERE id = :id")
    suspend fun getMusicById(id: String): MusicEntity?

    @Query("SELECT * FROM music WHERE LOWER(title) = LOWER(:title) AND formatId = :formatId")
    suspend fun getMusicByTitleAndFormat(title: String, formatId: String): MusicEntity?

    @Query("SELECT * FROM music WHERE barcode = :barcode")
    suspend fun getMusicByBarcode(barcode: String): MusicEntity?

    @Query("SELECT * FROM music WHERE isFavorite = 1")
    fun getFavoriteMusic(): Flow<List<MusicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusic(music: MusicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusicList(music: List<MusicEntity>)

    @Update
    suspend fun updateMusic(music: MusicEntity)

    @Delete
    suspend fun deleteMusic(music: MusicEntity)

    @Query("DELETE FROM music")
    suspend fun deleteAllMusic()
}
