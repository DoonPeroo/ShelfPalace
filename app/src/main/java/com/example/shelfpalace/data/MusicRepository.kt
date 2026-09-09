package com.example.shelfpalace.data

import com.example.shelfpalace.data.local.MusicDao
import com.example.shelfpalace.data.local.toEntity
import com.example.shelfpalace.data.local.toExternalModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MusicRepository(private val musicDao: MusicDao) {
    fun getMusicForFormat(formatId: String): Flow<List<Music>> {
        val flow = if (formatId == "all") musicDao.getAllMusic() else musicDao.getMusicForFormat(formatId)
        return flow.map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    fun searchMusic(query: String): Flow<List<Music>> =
        musicDao.searchMusic(query).map { entities ->
            entities.map { it.toExternalModel() }
        }

    fun getAllMusic(): Flow<List<Music>> =
        musicDao.getAllMusic().map { entities ->
            entities.map { it.toExternalModel() }
        }

    fun getMusicStream(id: String): Flow<Music?> =
        musicDao.getMusicStream(id).map { it?.toExternalModel() }

    suspend fun getMusicById(id: String): Music? =
        musicDao.getMusicById(id)?.toExternalModel()

    suspend fun doesMusicExist(title: String, formatId: String): Boolean {
        return musicDao.getMusicByTitleAndFormat(title, formatId) != null
    }

    suspend fun getMusicByBarcode(barcode: String): Music? =
        musicDao.getMusicByBarcode(barcode)?.toExternalModel()

    fun getFavoriteMusic(): Flow<List<Music>> =
        musicDao.getFavoriteMusic().map { entities ->
            entities.map { it.toExternalModel() }
        }

    suspend fun toggleFavorite(id: String) {
        val music = musicDao.getMusicById(id)
        if (music != null) {
            musicDao.updateMusic(music.copy(isFavorite = !music.isFavorite))
        }
    }

    suspend fun insertMusic(music: Music) = musicDao.insertMusic(music.toEntity())

    suspend fun insertMusicList(music: List<Music>) {
        musicDao.insertMusicList(music.map { it.toEntity() })
    }

    suspend fun updateMusic(music: Music) = musicDao.insertMusic(music.toEntity())

    suspend fun deleteMusic(music: Music) = musicDao.deleteMusic(music.toEntity())

    suspend fun clearAllMusic() {
        musicDao.deleteAllMusic()
    }
}
