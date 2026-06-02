package com.myreader.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myreader.app.data.local.dao.*
import com.myreader.app.data.local.entity.*

@Database(
    entities = [
        CachedBookEntity::class,
        DownloadedBookEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        ReadingHistoryEntity::class,
        PendingSyncEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MyReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun downloadDao(): DownloadDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
