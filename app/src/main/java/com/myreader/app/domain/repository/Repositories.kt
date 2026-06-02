package com.myreader.app.domain.repository

import android.net.Uri
import com.myreader.app.domain.model.*
import kotlinx.coroutines.flow.Flow

// ─── Auth Repository ──────────────────────────────────────────────────────
interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInAsGuest(): Result<User>
    suspend fun convertGuestToEmailAccount(email: String, password: String, name: String): Result<User>
    suspend fun signOut()
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun updateProfile(name: String, photoUri: Uri?): Result<User>
    suspend fun deleteAccount(): Result<Unit>
    fun isLoggedIn(): Boolean
}

// ─── Book Repository ──────────────────────────────────────────────────────
interface BookRepository {
    fun getFeaturedBooks(): Flow<List<Book>>
    fun getPopularBooks(limit: Int = 20): Flow<List<Book>>
    fun getNewlyAddedBooks(limit: Int = 20): Flow<List<Book>>
    fun getTopRatedBooks(limit: Int = 20): Flow<List<Book>>
    fun getMostDownloadedBooks(limit: Int = 20): Flow<List<Book>>
    fun getBooksByCategory(category: BookCategory): Flow<List<Book>>
    fun searchBooks(query: String): Flow<List<Book>>
    suspend fun getBookById(bookId: String): Result<Book>
    suspend fun addToFavorites(bookId: String): Result<Unit>
    suspend fun removeFromFavorites(bookId: String): Result<Unit>
    fun getFavoriteBooks(): Flow<List<Book>>
    fun isFavorite(bookId: String): Flow<Boolean>
    suspend fun incrementDownloadCount(bookId: String)
    suspend fun submitBookRequest(request: BookRequest): Result<Unit>
    suspend fun reportBrokenBook(bookId: String, reason: String): Result<Unit>
    suspend fun rateBook(bookId: String, rating: Float): Result<Unit>
}

// ─── Download Repository ──────────────────────────────────────────────────
interface DownloadRepository {
    fun getDownloadedBooks(): Flow<List<DownloadedBook>>
    suspend fun downloadBook(book: Book): Result<Unit>
    suspend fun deleteDownload(bookId: String): Result<Unit>
    suspend fun isBookDownloaded(bookId: String): Boolean
    fun getDownloadProgress(bookId: String): Flow<Int>
    suspend fun getLocalFilePath(bookId: String): String?
}

// ─── Reading Repository ───────────────────────────────────────────────────
interface ReadingRepository {
    fun getReadingProgress(bookId: String): Flow<ReadingProgress?>
    suspend fun saveReadingProgress(progress: ReadingProgress)
    fun getRecentlyRead(limit: Int = 10): Flow<List<Book>>
    fun getReadingHistory(): Flow<List<Book>>
    suspend fun addToHistory(bookId: String)
    fun getBookmarks(bookId: String): Flow<List<Bookmark>>
    suspend fun addBookmark(bookmark: Bookmark): Result<Bookmark>
    suspend fun deleteBookmark(bookmarkId: String): Result<Unit>
    suspend fun updateBookmark(bookmark: Bookmark): Result<Unit>
}

// ─── User Repository ──────────────────────────────────────────────────────
interface UserRepository {
    fun getUserProfile(): Flow<User?>
    suspend fun updateReadingStats(minutesRead: Long)
    fun getReadingStats(): Flow<ReadingStats>
    suspend fun setDailyGoal(minutes: Int): Result<Unit>
    suspend fun setWeeklyGoal(books: Int): Result<Unit>
    fun getAchievements(): Flow<List<Achievement>>
    suspend fun checkAndUnlockAchievements()
    suspend fun updateNotificationSettings(enabled: Boolean): Result<Unit>
    suspend fun updateLanguagePreference(lang: String): Result<Unit>
}

// ─── Admin Repository ─────────────────────────────────────────────────────
interface AdminRepository {
    suspend fun addBook(book: Book, coverUri: Uri, fileUri: Uri): Result<Book>
    suspend fun updateBook(book: Book, coverUri: Uri?, fileUri: Uri?): Result<Book>
    suspend fun deleteBook(bookId: String): Result<Unit>
    fun getAllUsers(): Flow<List<User>>
    suspend fun banUser(userId: String): Result<Unit>
    suspend fun sendPushNotification(title: String, body: String, targetUserIds: List<String>?): Result<Unit>
    fun getAdminStats(): Flow<AdminStats>
    fun getBookRequests(): Flow<List<BookRequest>>
    suspend fun updateBookRequestStatus(requestId: String, status: RequestStatus): Result<Unit>
}

data class AdminStats(
    val totalBooks: Int = 0,
    val totalUsers: Int = 0,
    val totalDownloads: Long = 0L,
    val newUsersToday: Int = 0,
    val downloadsToday: Long = 0L,
    val activeUsersToday: Int = 0,
    val popularCategories: List<Pair<BookCategory, Int>> = emptyList(),
)
