package com.example.shelfpalace.data

import com.example.shelfpalace.data.local.GameDao
import com.example.shelfpalace.data.local.toEntity
import com.example.shelfpalace.data.local.toExternalModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {
    fun getGamesForPlatform(platformId: String): Flow<List<Game>> {
        val flow = if (platformId == "all") gameDao.getAllGames() else gameDao.getGamesForPlatform(platformId)
        return flow.map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    fun searchGames(query: String): Flow<List<Game>> {
        return gameDao.searchGames(query).map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    fun getAllGames(): Flow<List<Game>> {
        return gameDao.getAllGames().map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    fun getGameStream(id: String): Flow<Game?> {
        return gameDao.getGameStream(id).map { it?.toExternalModel() }
    }

    suspend fun getGameById(id: String): Game? {
        return gameDao.getGameById(id)?.toExternalModel()
    }

    private fun isTitleDuplicate(existingTitle: String, enteredTitle: String): Boolean {
        val clean1 = existingTitle.lowercase().trim()
        val clean2 = enteredTitle.lowercase().trim()
        if (clean1 == clean2) return true
        val norm1 = clean1.replace("[^a-z0-9]".toRegex(), "")
        val norm2 = clean2.replace("[^a-z0-9]".toRegex(), "")
        return norm1.isNotEmpty() && norm1 == norm2
    }

    suspend fun doesGameExist(title: String, platformId: String): Boolean {
        return doesGameExistExcludingId(title, platformId, null)
    }

    suspend fun doesGameExistExcludingId(title: String, platformId: String, excludeId: String?): Boolean {
        if (title.isBlank()) return false
        val list = if (platformId.isBlank() || platformId == "all") {
            gameDao.getAllGamesList()
        } else {
            gameDao.getGamesForPlatformList(platformId)
        }
        return list.any { existing ->
            existing.id != excludeId && isTitleDuplicate(existing.title, title)
        }
    }

    suspend fun getGameByBarcode(barcode: String): Game? {
        return gameDao.getGameByBarcode(barcode)?.toExternalModel()
    }

    fun getFavoriteGames(): Flow<List<Game>> {
        return gameDao.getFavoriteGames().map { entities ->
            entities.map { it.toExternalModel() }
        }
    }

    suspend fun toggleFavorite(id: String) {
        gameDao.getGameById(id)?.let {
            gameDao.updateGame(it.copy(isFavorite = !it.isFavorite))
        }
    }

    suspend fun insertGame(game: Game) {
        gameDao.insertGame(game.toEntity())
    }

    suspend fun insertGames(games: List<Game>) {
        gameDao.insertGames(games.map { it.toEntity() })
    }

    suspend fun updateGame(game: Game) {
        gameDao.insertGame(game.toEntity())
    }

    suspend fun deleteGame(game: Game) {
        gameDao.deleteGame(game.toEntity())
    }

    suspend fun clearAllGames() {
        gameDao.deleteAllGames()
    }
}
