package com.myreader.app.data.local.entity

import androidx.room.*
import java.util.Date

// ─── Converters ───────────────────────────────────────────────────────────
class Converters {
    @TypeConverter fun fromDate(date: Date?): Long? = date?.time
    @TypeConverter fun toDate(ms: Long?): Date? = ms?.let { Date(it) }
    @TypeConverter fun fromList(list: List<String>): String = list.joinToString(",")
    @TypeConverter fun toList(str: String): List<String> = if (str.isBlank()) emptyList() else str.split(",")
}

// ─── Cached Book ──────────────────────────────────────────────────────────
@Entity(tableName = "cached_books")
data class CachedBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val coverUrl: String,
    val fileUrl: String,
    val fileFormat: String,
    val fileSizeBytes: Long,
    val totalPages: Int,
    val category: String,
    val language: String,
    val rating: Float,
    val ratingCount: Int,
    val downloadCount: Long,
    val tags: String,        // comma-separated
    val publishedYear: Int,
    val isFeatured: Boolean,
    val isActive: Boolean,
    val cachedAt: Date = Date(),
)

// ─── Downloaded Book ──────────────────────────────────────────────────────
@Entity(tableName = "downloaded_books")
data class DownloadedBookEntity(
    @PrimaryKey val bookId: String,
    val localFilePath: String,
    val downloadedAt: Date,
    val fileSizeBytes: Long,
    val format: String,
    val lastReadPage: Int = 0,
    val totalPages: Int = 0,
    val lastOpenedAt: Date? = null,
    // Denormalized book info for offline display
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val category: String = "",
)

// ─── Reading Progress ─────────────────────────────────────────────────────
@Entity(tableName = "reading_progress", primaryKeys = ["bookId", "userId"])
data class ReadingProgressEntity(
    val bookId: String,
    val userId: String,
    val currentPage: Int,
    val totalPages: Int,
    val scrollOffset: Int = 0,
    val lastReadAt: Date = Date(),
    val startedAt: Date = Date(),
    val totalReadingSeconds: Long = 0L,
)

// ─── Bookmark ─────────────────────────────────────────────────────────────
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val userId: String,
    val page: Int,
    val label: String,
    val note: String,
    val highlightedText: String,
    val highlightColor: String,
    val createdAt: Date = Date(),
)

// ─── Reading History ──────────────────────────────────────────────────────
@Entity(tableName = "reading_history")
data class ReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val userId: String,
    val openedAt: Date = Date(),
    val readingMinutes: Long = 0L,
)

// ─── Pending Sync ─────────────────────────────────────────────────────────
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,   // "PROGRESS", "BOOKMARK", "FAVORITE"
    val payload: String, // JSON
    val createdAt: Date = Date(),
)
