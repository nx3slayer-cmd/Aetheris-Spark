package com.kallistocore.ai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

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
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Performance PRAGMAs for low-latency on-device memory access
                        db.execSQL("PRAGMA journal_mode=WAL;")
                        db.execSQL("PRAGMA synchronous=NORMAL;")
                        db.execSQL("PRAGMA temp_store=MEMORY;")
                        db.execSQL("PRAGMA cache_size=-64000;") // 64 MB fast RAM cache
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
