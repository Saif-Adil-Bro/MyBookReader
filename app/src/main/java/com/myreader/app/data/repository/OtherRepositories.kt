package com.myreader.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.myreader.app.data.local.dao.ReadingProgressDao
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ─── UserRepositoryImpl ───────────────────────────────────────────────────
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val progressDao: ReadingProgressDao,
) : UserRepository {

    private val uid get() = auth.currentUser?.uid ?: "guest"

    override fun getUserProfile(): Flow<User?> = flow {
        val snap = firestore.collection("users").document(uid).get().await()
        emit(snap.toObject(User::class.java))
    }

    override suspend fun updateReadingStats(minutesRead: Long) {
        firestore.collection("users").document(uid)
            .update(
                "totalReadingMinutes", com.google.firebase.firestore.FieldValue.increment(minutesRead),
                "lastActiveDate", com.google.firebase.Timestamp.now()
            ).await()
    }

    override fun getReadingStats(): Flow<ReadingStats> = flow {
        emit(ReadingStats()) // populated from Firestore in real impl
    }

    override suspend fun setDailyGoal(minutes: Int): Result<Unit> = runCatching {
        firestore.collection("users").document(uid)
            .update("dailyReadingGoalMinutes", minutes).await()
    }

    override suspend fun setWeeklyGoal(books: Int): Result<Unit> = runCatching {
        firestore.collection("users").document(uid)
            .update("weeklyReadingGoalBooks", books).await()
    }

    override fun getAchievements(): Flow<List<Achievement>> = flow {
        emit(defaultAchievements())
    }

    override suspend fun checkAndUnlockAchievements() { /* Check thresholds */ }

    override suspend fun updateNotificationSettings(enabled: Boolean): Result<Unit> = runCatching {
        if (enabled) FirebaseMessaging.getInstance().subscribeToTopic("all").await()
        else FirebaseMessaging.getInstance().unsubscribeFromTopic("all").await()
        firestore.collection("users").document(uid)
            .update("notificationsEnabled", enabled).await()
    }

    override suspend fun updateLanguagePreference(lang: String): Result<Unit> = runCatching {
        firestore.collection("users").document(uid).update("preferredLanguage", lang).await()
    }

    private fun defaultAchievements() = listOf(
        Achievement("first_book", "First Book", "Read your first book", 0, false, null, 0, 1),
        Achievement("week_streak", "Week Warrior", "7-day reading streak", 0, false, null, 0, 7),
        Achievement("ten_books", "Bookworm", "Read 10 books", 0, false, null, 0, 10),
        Achievement("hundred_pages", "Page Turner", "Read 100 pages in a day", 0, false, null, 0, 100),
        Achievement("night_owl", "Night Owl", "Read after midnight", 0, false, null, 1, 1),
        Achievement("downloader", "Collector", "Download 5 books", 0, false, null, 0, 5),
    )
}

// ─── AdminRepositoryImpl ──────────────────────────────────────────────────
@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: com.google.firebase.storage.FirebaseStorage,
    private val messaging: FirebaseMessaging,
) : AdminRepository {

    override suspend fun addBook(book: Book, coverUri: Uri, fileUri: Uri): Result<Book> = runCatching {
        // Upload cover
        val coverRef = storage.reference.child("covers/${System.currentTimeMillis()}_cover")
        coverRef.putFile(coverUri).await()
        val coverUrl = coverRef.downloadUrl.await().toString()

        // Upload file
        val fileExt = if (book.fileFormat == BookFormat.EPUB) "epub" else "pdf"
        val fileRef = storage.reference.child("books/${System.currentTimeMillis()}_book.$fileExt")
        fileRef.putFile(fileUri).await()
        val fileUrl = fileRef.downloadUrl.await().toString()

        val newBook = book.copy(coverUrl = coverUrl, fileUrl = fileUrl)
        val ref = firestore.collection("books").add(newBook).await()
        newBook.copy(id = ref.id)
    }

    override suspend fun updateBook(book: Book, coverUri: Uri?, fileUri: Uri?): Result<Book> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "title" to book.title, "author" to book.author,
            "description" to book.description, "category" to book.category.name,
        )
        coverUri?.let {
            val ref = storage.reference.child("covers/${book.id}_cover")
            ref.putFile(it).await()
            updates["coverUrl"] = ref.downloadUrl.await().toString()
        }
        fileUri?.let {
            val ref = storage.reference.child("books/${book.id}_file")
            ref.putFile(it).await()
            updates["fileUrl"] = ref.downloadUrl.await().toString()
        }
        firestore.collection("books").document(book.id).update(updates).await()
        book
    }

    override suspend fun deleteBook(bookId: String): Result<Unit> = runCatching {
        firestore.collection("books").document(bookId).delete().await()
    }

    override fun getAllUsers(): Flow<List<User>> = flow {
        val snap = firestore.collection("users").get().await()
        emit(snap.toObjects(User::class.java))
    }

    override suspend fun banUser(userId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(userId).update("banned", true).await()
    }

    override suspend fun sendPushNotification(
        title: String, body: String, targetUserIds: List<String>?
    ): Result<Unit> = runCatching {
        // Subscribe all or specific users via FCM topic
        firestore.collection("admin_notifications").add(
            mapOf("title" to title, "body" to body,
                "targetUserIds" to (targetUserIds ?: emptyList<String>()),
                "createdAt" to com.google.firebase.Timestamp.now())
        ).await()
        // Trigger Cloud Function (set up in Firebase Console)
    }

    override fun getAdminStats(): Flow<AdminStats> = flow {
        val books = firestore.collection("books").get().await()
        val users = firestore.collection("users").get().await()
        emit(AdminStats(
            totalBooks = books.size(),
            totalUsers = users.size(),
        ))
    }

    override fun getBookRequests(): Flow<List<BookRequest>> = flow {
        val snap = firestore.collection("book_requests")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await()
        emit(snap.toObjects(BookRequest::class.java))
    }

    override suspend fun updateBookRequestStatus(requestId: String, status: RequestStatus): Result<Unit> = runCatching {
        firestore.collection("book_requests").document(requestId)
            .update("status", status.name).await()
    }
}
