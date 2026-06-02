package com.myreader.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myreader.app.data.local.dao.*
import com.myreader.app.data.local.entity.*
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.BookRepository
import com.myreader.app.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRepositoryImpl @Inject constructor(
    private val progressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: ReadingHistoryDao,
    private val bookDao: BookDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ReadingRepository {

    private val uid get() = auth.currentUser?.uid ?: "guest"

    override fun getReadingProgress(bookId: String): Flow<ReadingProgress?> =
        progressDao.getProgress(bookId, uid).map { it?.toDomain() }

    override suspend fun saveReadingProgress(progress: ReadingProgress) {
        progressDao.upsert(progress.toEntity(uid))
        // Sync to Firestore (best effort)
        try {
            firestore.collection("users").document(uid)
                .collection("progress").document(progress.bookId)
                .set(progress).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync progress")
        }
    }

    override fun getRecentlyRead(limit: Int): Flow<List<Book>> =
        historyDao.getRecentBookIds(uid, limit).flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else bookDao.getAllBooks().map { all -> all.filter { it.id in ids }.map { it.toDomain() } }
        }

    override fun getReadingHistory(): Flow<List<Book>> =
        historyDao.getRecentBookIds(uid, 50).flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else bookDao.getAllBooks().map { all -> all.filter { it.id in ids }.map { it.toDomain() } }
        }

    override suspend fun addToHistory(bookId: String) {
        historyDao.insertHistory(
            ReadingHistoryEntity(bookId = bookId, userId = uid, openedAt = Date())
        )
    }

    override fun getBookmarks(bookId: String): Flow<List<Bookmark>> =
        bookmarkDao.getBookmarks(bookId).map { list -> list.map { it.toDomain() } }

    override suspend fun addBookmark(bookmark: Bookmark): Result<Bookmark> = runCatching {
        val entity = bookmark.toEntity(uid)
        bookmarkDao.upsert(entity)
        // Sync Firestore
        firestore.collection("users").document(uid)
            .collection("bookmarks").document(bookmark.id)
            .set(bookmark).await()
        bookmark
    }

    override suspend fun deleteBookmark(bookmarkId: String): Result<Unit> = runCatching {
        val entity = bookmarkDao.getById(bookmarkId) ?: return@runCatching
        bookmarkDao.delete(entity)
        firestore.collection("users").document(uid)
            .collection("bookmarks").document(bookmarkId)
            .delete().await()
    }

    override suspend fun updateBookmark(bookmark: Bookmark): Result<Unit> = runCatching {
        bookmarkDao.upsert(bookmark.toEntity(uid))
    }

    // ── Mappers ───────────────────────────────────────────────────────────
    private fun ReadingProgressEntity.toDomain() = ReadingProgress(
        bookId = bookId, userId = userId,
        currentPage = currentPage, totalPages = totalPages,
        scrollOffset = scrollOffset, lastReadAt = lastReadAt,
        startedAt = startedAt, totalReadingMinutes = totalReadingSeconds / 60,
    )

    private fun ReadingProgress.toEntity(uid: String) = ReadingProgressEntity(
        bookId = bookId, userId = uid,
        currentPage = currentPage, totalPages = totalPages,
        scrollOffset = scrollOffset, lastReadAt = lastReadAt,
        startedAt = startedAt, totalReadingSeconds = totalReadingMinutes * 60,
    )

    private fun BookmarkEntity.toDomain() = Bookmark(
        id = id, bookId = bookId, userId = userId, page = page,
        label = label, note = note, highlightedText = highlightedText,
        highlightColor = highlightColor, createdAt = createdAt,
    )

    private fun Bookmark.toEntity(uid: String) = BookmarkEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        bookId = bookId, userId = uid, page = page,
        label = label, note = note, highlightedText = highlightedText,
        highlightColor = highlightColor, createdAt = createdAt,
    )

    private fun CachedBookEntity.toDomain() = Book(
        id = id, title = title, author = author, description = description,
        coverUrl = coverUrl, fileUrl = fileUrl,
        fileFormat = BookFormat.valueOf(fileFormat),
        fileSizeBytes = fileSizeBytes, totalPages = totalPages,
        category = BookCategory.fromString(category),
        rating = rating, ratingCount = ratingCount, downloadCount = downloadCount,
        isFeatured = isFeatured, isActive = isActive,
    )
}
