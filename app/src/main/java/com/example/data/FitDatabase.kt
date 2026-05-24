package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ActivityEntity::class,
        BadgeEntity::class,
        ChallengeEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FitDatabase : RoomDatabase() {
    abstract val fitDao: FitDao

    companion object {
        @Volatile
        private var INSTANCE: FitDatabase? = null

        fun getDatabase(context: Context): FitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitDatabase::class.java,
                    "fit_quest_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
