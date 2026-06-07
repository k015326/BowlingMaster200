package com.example.bowlingmaster200.data.repository

import android.content.Context
import com.example.bowlingmaster200.data.local.BowlingDatabase
import com.example.bowlingmaster200.data.local.dao.BowlingDao
import com.example.bowlingmaster200.data.local.entity.FrameEntity
import com.example.bowlingmaster200.data.local.entity.GameEntity
import com.example.bowlingmaster200.data.mapper.BowlingMapper
import com.example.bowlingmaster200.domain.model.GameMetadata
import com.example.bowlingmaster200.domain.model.SavedGame
import com.example.bowlingmaster200.domain.model.SavedGameDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UI / ViewModel からの唯一のデータアクセス窓口。
 * DAO を直接公開しない。
 */
class BowlingRepository(
    private val dao: BowlingDao,
) {

    fun observeAllGames(): Flow<List<GameMetadata>> {
        return dao.getAllGames().map { games ->
            games.map(BowlingMapper::toGameMetadata)
        }
    }

    suspend fun getGameDetail(gameId: Long): SavedGameDetail? {
        return dao.getGameDetail(gameId)?.let(BowlingMapper::toSavedGameDetail)
    }

    suspend fun getSavedGame(gameId: Long): SavedGame? {
        return dao.getGameDetail(gameId)?.let(BowlingMapper::toSavedGame)
    }

    suspend fun saveGame(savedGame: SavedGame): Long {
        val (gameEntity, frameEntities) = BowlingMapper.toPersistedPair(savedGame)
        return dao.insertGameWithFrames(gameEntity, frameEntities)
    }

    suspend fun insertGame(game: GameEntity): Long {
        return dao.insertGame(game)
    }

    suspend fun insertFrames(frames: List<FrameEntity>) {
        dao.insertFrames(frames)
    }

    suspend fun insertGameWithFrames(game: GameEntity, frames: List<FrameEntity>): Long {
        return dao.insertGameWithFrames(game, frames)
    }

    suspend fun deleteGame(gameId: Long) {
        dao.deleteGame(gameId)
    }

    companion object {
        fun create(context: Context): BowlingRepository {
            val database = BowlingDatabase.getInstance(context)
            return BowlingRepository(database.bowlingDao())
        }
    }
}
