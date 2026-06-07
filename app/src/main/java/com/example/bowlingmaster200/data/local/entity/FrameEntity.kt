package com.example.bowlingmaster200.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "frames",
    primaryKeys = ["gameId", "frameIndex"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gameId")],
)
data class FrameEntity(
    val gameId: Long,
    val frameIndex: Int,
    val firstRoll: Int? = null,
    val secondRoll: Int? = null,
    val bonusRoll: Int? = null,
    val frameType: String,
    val frameScore: Int? = null,
    val cumulativeScore: Int? = null,
)
