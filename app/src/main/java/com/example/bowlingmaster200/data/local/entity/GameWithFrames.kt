package com.example.bowlingmaster200.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class GameWithFrames(
    @Embedded
    val game: GameEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "gameId",
    )
    val frames: List<FrameEntity>,
)
