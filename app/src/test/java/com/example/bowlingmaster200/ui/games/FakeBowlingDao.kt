package com.example.bowlingmaster200.ui.games

import com.example.bowlingmaster200.data.local.dao.BowlingDao
import com.example.bowlingmaster200.data.local.entity.FrameEntity
import com.example.bowlingmaster200.data.local.entity.GameEntity
import com.example.bowlingmaster200.data.local.entity.GameWithFrames
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeBowlingDao : BowlingDao {
    var insertGameWithFramesCallCount: Int = 0
        private set

    private val games = mutableListOf<GameEntity>()
    private val frames = mutableListOf<FrameEntity>()
    private var nextGameId = 1L

    override suspend fun insertGame(game: GameEntity): Long {
        val id = nextGameId++
        games.add(game.copy(id = id))
        return id
    }

    override suspend fun insertFrames(frameList: List<FrameEntity>) {
        frames.addAll(frameList)
    }

    override suspend fun insertGameWithFrames(
        game: GameEntity,
        frameList: List<FrameEntity>,
    ): Long {
        insertGameWithFramesCallCount++
        val gameId = insertGame(game)
        insertFrames(frameList.map { it.copy(gameId = gameId) })
        return gameId
    }

    override fun getAllGames(): Flow<List<GameEntity>> = flowOf(games.toList())

    override suspend fun getGameDetail(gameId: Long): GameWithFrames? {
        val game = games.find { it.id == gameId } ?: return null
        return GameWithFrames(
            game = game,
            frames = frames.filter { it.gameId == gameId },
        )
    }

    override suspend fun deleteGame(gameId: Long) {
        games.removeAll { it.id == gameId }
        frames.removeAll { it.gameId == gameId }
    }
}
