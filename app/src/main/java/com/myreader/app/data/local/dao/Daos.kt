package com.myreader.app.data.local.dao

import androidx.room.*
import com.myreader.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ─── Book DAO ─────────────────────────────────────────────────────────────
@Dao
interface BookDao {
    @Upsert suspend fun upsertBooks(books: List<CachedBookEntity>)
    @Query("SELECT * FROM cached_books WHERE id = :id") suspend fun getById(id: String): CachedBookEntity?
    @Query("SELECT * FROM cached_books WHERE isActive = 1 ORDER BY cachedAt DESC")
    fun getAllBooks(): Flow<List<CachedBookEntity>>
    @Query("SELECT * FROM cached_books WHERE isFeatured = 1 LIMIT 10")
    fun getFeatured(): Flow<List<CachedBookEntity>>
    @Query("SELECT * FROM cached_books WHERE isActive = 1 ORDER BY downloadCount DESC LIMIT :limit")
    fun getPopular(limit: Int): Flow<List<CachedBookEntity>>
    @Query("SELECT * FROM cached_books WHERE isActive = 1 ORDER BY cachedAt DESC LIMIT :limit")
    fun getNewlyAdded(limit: Int): Flow<List<CachedBookEntity>>
    @Query("SELECT * FROM cached_books WHERE category = :category AND isActive = 1")
    fun getByCategory(category: String): Flow<List<CachedBookEntity>>
    @Query("SELECT * FROM cached_books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<CachedBookEntity>>
    @Query("DELETE FROM cached_books") suspend fun clearAll()
}

// ─── Download DAO ─────────────────────────────────────────────────────────
@Dao
interface DownloadDao {
    @Upsert suspend fun upsertDownload(download: DownloadedBookEntity)
    @Delete suspend fun deleteDownload(download: DownloadedBookEntity)
    @Query("SELECT * FROM downloaded_books ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadedBookEntity>>
    @Query("SELECT * FROM downloaded_books WHERE bookId = :bookId")
    suspend fun getDownload(bookId: String): DownloadedBookEntity?
    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_books WHERE bookId = :bookId)")
    suspend fun isDownloaded(bookId: String): Boolean
    @Query("UPDATE downloaded_books SET lastReadPage = :page, lastOpenedAt = :openedAt WHERE bookId = :bookId")
    suspend fun updateLastRead(bookId: String, page: Int, openedAt: Long)
    @Query("SELECT COUNT(*) FROM downloaded_books") suspend fun getDownloadCount(): Int
}

// ─── Reading Progress DAO ─────────────────────────────────────────────────
@Dao
interface ReadingProgressDao {
    @Upsert suspend fun upsert(progress: ReadingProgressEntity)
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId AND userId = :userId")
    fun getProgress(bookId: String, userId: String): Flow<ReadingProgressEntity?>
    @Query("SELECT * FROM reading_progress WHERE userId = :userId ORDER BY lastReadAt DESC LIMIT :limit")
    fun getRecentlyRead(userId: String, limit: Int): Flow<List<ReadingProgressEntity>>
    @Query("UPDATE reading_progress SET totalReadingSeconds = totalReadingSeconds + :seconds WHERE bookId = :bookId AND userId = :userId")
    suspend fun addReadingTime(bookId: String, userId: String, seconds: Long)
    @Query("SELECT SUM(totalReadingSeconds) FROM reading_progress WHERE userId = :userId")
    suspend fun getTotalReadingSeconds(userId: String): Long?
}

// ─── Bookmark DAO ─────────────────────────────────────────────────────────
@Dao
interface BookmarkDao {
    @Upsert suspend fun upsert(bookmark: BookmarkEntity)
    @Delete suspend fun delete(bookmark: BookmarkEntity)
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY page ASC")
    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>>
    @Query("SELECT * FROM bookmarks WHERE id = :id") suspend fun getById(id: String): BookmarkEntity?
    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND userId = :userId")
    suspend fun deleteAllForBook(bookId: String, userId: String)
    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId") suspend fun countForBook(bookId: String): Int
}

// ─── Reading History DAO ──────────────────────────────────────────────────
@Dao
interface ReadingHistoryDao {
    @Insert suspend fun insertHistory(history: ReadingHistoryEntity)
    @Query("SELECT DISTINCT bookId FROM reading_history WHERE userId = :userId ORDER BY openedAt DESC LIMIT :limit")
    fun getRecentBookIds(userId: String, limit: Int): Flow<List<String>>
    @Query("SELECT SUM(readingMinutes) FROM reading_history WHERE userId = :userId AND openedAt >= :since")
    suspend fun getTotalMinutesSince(userId: String, since: Long): Long?
    @Query("SELECT * FROM reading_history WHERE userId = :userId ORDER BY openedAt DESC")
    fun getAllHistory(userId: String): Flow<List<ReadingHistoryEntity>>
    @Query("DELETE FROM reading_history WHERE userId = :userId") suspend fun clearHistory(userId: String)
}

// ─── Pending Sync DAO ─────────────────────────────────────────────────────
@Dao
interface PendingSyncDao {
    @Insert suspend fun insert(item: PendingSyncEntity)
    @Delete suspend fun delete(item: PendingSyncEntity)
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC LIMIT 50")
    suspend fun getPending(): List<PendingSyncEntity>
    @Query("DELETE FROM pending_sync WHERE id IN (:ids)") suspend fun deleteByIds(ids: List<Long>)
}
