package com.myreader.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.AdminRepository
import com.myreader.app.domain.repository.AdminStats
import com.myreader.app.domain.repository.BookRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : BookRepository {

    companion object {
        const val BOOKS_COL       = "books"
        const val FAVORITES_COL   = "favorites"
        const val RATINGS_COL     = "ratings"
        const val REPORTS_COL     = "reports"
        const val REQUESTS_COL    = "book_requests"
    }

    private val booksRef get() = firestore.collection(BOOKS_COL)
    private val uid get() = auth.currentUser?.uid ?: ""

    // ── Queries ────────────────────────────────────────────────────────────
    override fun getFeaturedBooks(): Flow<List<Book>> = booksRef
        .whereEqualTo("featured", true)
        .whereEqualTo("active", true)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(10)
        .snapshots()

    override fun getPopularBooks(limit: Int): Flow<List<Book>> = booksRef
        .whereEqualTo("active", true)
        .orderBy("downloadCount", Query.Direction.DESCENDING)
        .limit(limit.toLong())
        .snapshots()

    override fun getNewlyAddedBooks(limit: Int): Flow<List<Book>> = booksRef
        .whereEqualTo("active", true)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit.toLong())
        .snapshots()

    override fun getTopRatedBooks(limit: Int): Flow<List<Book>> = booksRef
        .whereEqualTo("active", true)
        .whereGreaterThan("ratingCount", 5)
        .orderBy("ratingCount", Query.Direction.DESCENDING)
        .orderBy("rating", Query.Direction.DESCENDING)
        .limit(limit.toLong())
        .snapshots()

    override fun getMostDownloadedBooks(limit: Int): Flow<List<Book>> = booksRef
        .whereEqualTo("active", true)
        .orderBy("downloadCount", Query.Direction.DESCENDING)
        .limit(limit.toLong())
        .snapshots()

    override fun getBooksByCategory(category: BookCategory): Flow<List<Book>> = booksRef
        .whereEqualTo("category", category.name)
        .whereEqualTo("active", true)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()

    override fun searchBooks(query: String): Flow<List<Book>> {
        if (query.isBlank()) return flowOf(emptyList())
        val lowerQuery = query.lowercase().trim()
        return booksRef
            .whereEqualTo("active", true)
            .orderBy("titleLower")
            .startAt(lowerQuery)
            .endAt("$lowerQuery\uf8ff")
            .limit(30)
            .snapshots()
    }

    override suspend fun getBookById(bookId: String): Result<Book> = runCatching {
        val doc = booksRef.document(bookId).get().await()
        doc.toObject(Book::class.java)?.copy(id = doc.id)
            ?: throw Exception("Book not found")
    }

    // ── Favorites ─────────────────────────────────────────────────────────
    override suspend fun addToFavorites(bookId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(uid)
            .collection(FAVORITES_COL).document(bookId)
            .set(mapOf("bookId" to bookId, "addedAt" to com.google.firebase.Timestamp.now()))
            .await()
    }

    override suspend fun removeFromFavorites(bookId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(uid)
            .collection(FAVORITES_COL).document(bookId)
            .delete().await()
    }

    override fun getFavoriteBooks(): Flow<List<Book>> = callbackFlow {
        val favRef = firestore.collection("users").document(uid)
            .collection(FAVORITES_COL)
        val sub = favRef.addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(emptyList()); return@addSnapshotListener }
            val bookIds = snapshot?.documents?.mapNotNull { it.getString("bookId") } ?: emptyList()
            if (bookIds.isEmpty()) { trySend(emptyList()); return@addSnapshotListener }
            // Batch fetch books
            firestore.collection(BOOKS_COL)
                .whereIn(FieldPath.documentId(), bookIds.take(10))
                .get()
                .addOnSuccessListener { booksSnap ->
                    trySend(booksSnap.toObjects(Book::class.java))
                }
                .addOnFailureListener { trySend(emptyList()) }
        }
        awaitClose { sub.remove() }
    }

    override fun isFavorite(bookId: String): Flow<Boolean> = callbackFlow {
        val ref = firestore.collection("users").document(uid)
            .collection(FAVORITES_COL).document(bookId)
        val sub = ref.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.exists() == true)
        }
        awaitClose { sub.remove() }
    }

    override suspend fun incrementDownloadCount(bookId: String) {
        try {
            booksRef.document(bookId)
                .update("downloadCount", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to increment download count")
        }
    }

    override suspend fun submitBookRequest(request: BookRequest): Result<Unit> = runCatching {
        firestore.collection(REQUESTS_COL)
            .add(request.copy(userId = uid)).await()
    }

    override suspend fun reportBrokenBook(bookId: String, reason: String): Result<Unit> = runCatching {
        firestore.collection(REPORTS_COL).add(
            mapOf(
                "bookId" to bookId,
                "userId" to uid,
                "reason" to reason,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
        ).await()
    }

    override suspend fun rateBook(bookId: String, rating: Float): Result<Unit> = runCatching {
        val ratingRef = booksRef.document(bookId)
            .collection(RATINGS_COL).document(uid)
        val existing = ratingRef.get().await()
        val wasRated = existing.exists()
        ratingRef.set(mapOf("rating" to rating, "userId" to uid)).await()

        // Update aggregate rating using transaction
        firestore.runTransaction { tx ->
            val bookDoc = tx.get(booksRef.document(bookId))
            val currentRating = bookDoc.getDouble("rating") ?: 0.0
            val currentCount = bookDoc.getLong("ratingCount") ?: 0L
            val newCount = if (wasRated) currentCount else currentCount + 1
            val newRating = if (wasRated) {
                // Recalculate (approximate)
                ((currentRating * currentCount) - (existing.getDouble("rating") ?: 0.0) + rating) / currentCount
            } else {
                ((currentRating * currentCount) + rating) / newCount
            }
            tx.update(booksRef.document(bookId),
                mapOf("rating" to newRating, "ratingCount" to newCount))
        }.await()
    }

    // ── Extension to convert Firestore snapshots to Flow ─────────────────
    private fun Query.snapshots(): Flow<List<Book>> = callbackFlow {
        val sub = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Firestore listen failed")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val books = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Book::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(books)
        }
        awaitClose { sub.remove() }
    }
}
