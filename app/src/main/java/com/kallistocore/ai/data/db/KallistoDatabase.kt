package com.kallistocore.ai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationSessionEntity::class,
        MessageEntity::class,
        MemoryEntryEntity::class,
        MemoryFtsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KallistoDatabase : RoomDatabase() {

    abstract fun memoryBankDao(): MemoryBankDao

    companion object {
        @Volatile
        private var INSTANCE: KallistoDatabase? = null
        private const val DATABASE_NAME = "kallisto_memory_bank.db"

        fun getInstance(context: Context): KallistoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KallistoDatabase::class.java,
                    DATABASE_NAME
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
