package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    entities = [Folder::class, Conversation::class, Message::class, MemoryItem::class, UserAccount::class, ErrorLog::class, MessageFeedback::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun errorLogDao(): ErrorLogDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `message_feedback` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `isPositive` INTEGER NOT NULL,
                        `category` TEXT,
                        `details` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nabih_ai_database"
                )
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

