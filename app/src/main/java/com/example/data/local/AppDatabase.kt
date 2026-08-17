package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room Database for Nabih AI.
 *
 * NOTE FOR DEVELOPERS:
 * Schema export is enabled (exportSchema = true).
 * Any changes to entity structures or table schemas MUST include an explicit [androidx.room.migration.Migration]
 * object registered via .addMigrations(...) prior to bumping the database version number.
 * Do NOT re-enable destructive migration on upgrade, as this will erase all user chat history and accounts.
 */
@Database(
    entities = [Folder::class, Conversation::class, Message::class, MemoryItem::class, UserAccount::class, ErrorLog::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun errorLogDao(): ErrorLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nabih_ai_database"
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
