package com.example.bowlingmaster200.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val playedAt: Long,
    val location: String? = null,
    val laneNumber: Int? = null,
    val ballName: String? = null,
    val totalScore: Int? = null,
    val createdAt: Long,
)
