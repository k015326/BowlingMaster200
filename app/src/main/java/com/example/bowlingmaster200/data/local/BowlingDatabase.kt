package com.example.bowlingmaster200.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.bowlingmaster200.data.local.dao.BowlingDao
import com.example.bowlingmaster200.data.local.entity.FrameEntity
import com.example.bowlingmaster200.data.local.entity.GameEntity

@Database(
    entities = [
        GameEntity::class,
        FrameEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class BowlingDatabase : RoomDatabase() {

    abstract fun bowlingDao(): BowlingDao

    companion object {
        private const val DATABASE_NAME = "bowling_master200.db"

        @Volatile
        private var instance: BowlingDatabase? = null

        fun getInstance(context: Context): BowlingDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BowlingDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
        }
    }
}
