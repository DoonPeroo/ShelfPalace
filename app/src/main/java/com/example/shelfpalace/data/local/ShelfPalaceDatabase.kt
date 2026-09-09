package com.example.shelfpalace.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameEntity::class, MovieEntity::class, MusicEntity::class], version = 21, exportSchema = false)
abstract class ShelfPalaceDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun movieDao(): MovieDao
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: ShelfPalaceDatabase? = null

        fun getDatabase(context: Context): ShelfPalaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShelfPalaceDatabase::class.java,
                    "shelfpalace_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
