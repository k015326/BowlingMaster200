package com.example.bowlingmaster200.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.bowlingmaster200.data.local.entity.FrameEntity
import com.example.bowlingmaster200.data.local.entity.GameEntity
import com.example.bowlingmaster200.data.local.entity.GameWithFrames
import kotlinx.coroutines.flow.Flow

@Dao
interface BowlingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrames(frames: List<FrameEntity>)

    @Transaction
    suspend fun insertGameWithFrames(game: GameEntity, frames: List<FrameEntity>): Long {
        val gameId = insertGame(game)
        val framesWithGameId = frames.map { it.copy(gameId = gameId) }
        insertFrames(framesWithGameId)
        return gameId
    }

    @Query("SELECT * FROM games ORDER BY playedAt DESC, createdAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Transaction
    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameDetail(gameId: Long): GameWithFrames?

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGame(gameId: Long)
}
