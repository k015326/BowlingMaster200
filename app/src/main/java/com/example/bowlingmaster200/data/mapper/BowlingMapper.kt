package com.example.bowlingmaster200.data.mapper

import com.example.bowlingmaster200.data.local.entity.FrameEntity
import com.example.bowlingmaster200.data.local.entity.GameEntity
import com.example.bowlingmaster200.data.local.entity.GameWithFrames
import com.example.bowlingmaster200.domain.calculator.BowlingScoreCalculator
import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.FrameScore
import com.example.bowlingmaster200.domain.model.FrameType
import com.example.bowlingmaster200.domain.model.GameMetadata
import com.example.bowlingmaster200.domain.model.GameScore
import com.example.bowlingmaster200.domain.model.SavedGame
import com.example.bowlingmaster200.domain.model.SavedGameDetail

object BowlingMapper {

    fun toGameEntity(metadata: GameMetadata): GameEntity {
        return GameEntity(
            id = metadata.id,
            playedAt = metadata.playedAt,
            location = metadata.location,
            laneNumber = metadata.laneNumber,
            ballName = metadata.ballName,
            totalScore = metadata.totalScore,
            createdAt = metadata.createdAt,
        )
    }

    fun toGameMetadata(entity: GameEntity): GameMetadata {
        return GameMetadata(
            id = entity.id,
            playedAt = entity.playedAt,
            location = entity.location,
            laneNumber = entity.laneNumber,
            ballName = entity.ballName,
            totalScore = entity.totalScore,
            createdAt = entity.createdAt,
        )
    }

    fun toFrameEntity(gameId: Long, frameScore: FrameScore): FrameEntity {
        return FrameEntity(
            gameId = gameId,
            frameIndex = frameScore.frameIndex,
            firstRoll = frameScore.firstRoll,
            secondRoll = frameScore.secondRoll,
            bonusRoll = frameScore.bonusRoll,
            frameType = frameScore.frameType.name,
            frameScore = frameScore.framePins,
            cumulativeScore = frameScore.cumulativeScore,
        )
    }

    fun toFrameEntities(gameId: Long, gameScore: GameScore): List<FrameEntity> {
        return gameScore.frameScores.map { toFrameEntity(gameId, it) }
    }

    fun toFrame(entity: FrameEntity): Frame {
        return Frame(
            firstRoll = entity.firstRoll,
            secondRoll = entity.secondRoll,
            bonusRoll = entity.bonusRoll,
        )
    }

    fun toFrameScore(entity: FrameEntity): FrameScore {
        return FrameScore(
            frameIndex = entity.frameIndex,
            firstRoll = entity.firstRoll,
            secondRoll = entity.secondRoll,
            bonusRoll = entity.bonusRoll,
            frameType = runCatching { FrameType.valueOf(entity.frameType) }
                .getOrDefault(FrameType.INCOMPLETE),
            framePins = entity.frameScore,
            cumulativeScore = entity.cumulativeScore,
        )
    }

    fun toGameScore(entities: List<FrameEntity>, totalScore: Int?): GameScore {
        val frameScores = entities
            .sortedBy { it.frameIndex }
            .map { toFrameScore(it) }
        val isComplete = frameScores.all { it.cumulativeScore != null }
        return GameScore(
            frameScores = frameScores,
            totalScore = totalScore ?: if (isComplete) frameScores.lastOrNull()?.cumulativeScore else null,
            isComplete = isComplete,
        )
    }

    fun toSavedGameDetail(relation: GameWithFrames): SavedGameDetail {
        val sortedFrames = relation.frames.sortedBy { it.frameIndex }
        return SavedGameDetail(
            metadata = toGameMetadata(relation.game),
            gameScore = toGameScore(sortedFrames, relation.game.totalScore),
        )
    }

    fun toSavedGame(relation: GameWithFrames): SavedGame {
        val frames = relation.frames
            .sortedBy { it.frameIndex }
            .map { toFrame(it) }
        return SavedGame(
            metadata = toGameMetadata(relation.game),
            frames = frames,
        )
    }

    /**
     * 保存用: domain → entity（スコアは Calculator で再計算して永続化）。
     */
    fun toPersistedPair(savedGame: SavedGame): Pair<GameEntity, List<FrameEntity>> {
        val gameScore = BowlingScoreCalculator.calculateGameScore(savedGame.frames)
        val metadata = savedGame.metadata.copy(totalScore = gameScore.totalScore)
        val gameEntity = toGameEntity(metadata)
        val frameEntities = toFrameEntities(gameId = 0L, gameScore)
        return gameEntity to frameEntities
    }
}
