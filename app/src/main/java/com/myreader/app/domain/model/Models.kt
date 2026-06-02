package com.myreader.app.domain.model

import java.util.Date

// ─── Book ─────────────────────────────────────────────────────────────────
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val fileUrl: String = "",
    val fileFormat: BookFormat = BookFormat.PDF,
    val fileSizeBytes: Long = 0L,
    val totalPages: Int = 0,
    val category: BookCategory = BookCategory.OTHER,
    val language: BookLanguage = BookLanguage.ENGLISH,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val downloadCount: Long = 0L,
    val viewCount: Long = 0L,
    val tags: List<String> = emptyList(),
    val publishedYear: Int = 0,
    val isbn: String = "",
    val publisher: String = "",
    val isFeatured: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) {
    val fileSizeMb: String get() {
        val mb = fileSizeBytes / (1024.0 * 1024.0)
        return "%.1f MB".format(mb)
    }
}

enum class BookFormat { PDF, EPUB }

enum class BookCategory(val displayNameEn: String, val displayNameBn: String) {
    ISLAMIC("Islamic Books", "ইসলামিক বই"),
    NOVELS("Novels", "উপন্যাস"),
    EDUCATIONAL("Educational", "শিক্ষামূলক"),
    SCIENCE("Science", "বিজ্ঞান"),
    HISTORY("History", "ইতিহাস"),
    TECHNOLOGY("Technology", "প্রযুক্তি"),
    BIOGRAPHY("Biography", "জীবনী"),
    CHILDREN("Children's Books", "শিশুদের বই"),
    SELF_DEVELOPMENT("Self Development", "আত্মউন্নয়ন"),
    OTHER("Other", "অন্যান্য");

    companion object {
        fun fromString(value: String): BookCategory =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class BookLanguage(val displayName: String) {
    ENGLISH("English"),
    BENGALI("বাংলা"),
    ARABIC("العربية"),
    BOTH("English & বাংলা")
}

// ─── Download ─────────────────────────────────────────────────────────────
data class DownloadedBook(
    val bookId: String,
    val localFilePath: String,
    val downloadedAt: Date,
    val fileSizeBytes: Long,
    val format: BookFormat,
    val lastReadPage: Int = 0,
    val totalPages: Int = 0,
    val lastOpenedAt: Date? = null,
) {
    val progress: Float get() =
        if (totalPages > 0) lastReadPage.toFloat() / totalPages else 0f
}

// ─── Reading Progress ─────────────────────────────────────────────────────
data class ReadingProgress(
    val bookId: String,
    val userId: String,
    val currentPage: Int,
    val totalPages: Int,
    val scrollOffset: Int = 0,
    val lastReadAt: Date = Date(),
    val startedAt: Date = Date(),
    val totalReadingMinutes: Long = 0L,
) {
    val progressPercent: Int get() =
        if (totalPages > 0) ((currentPage.toFloat() / totalPages) * 100).toInt() else 0
}

// ─── Bookmark ─────────────────────────────────────────────────────────────
data class Bookmark(
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val page: Int = 0,
    val label: String = "",
    val note: String = "",
    val highlightedText: String = "",
    val highlightColor: String = "YELLOW",
    val createdAt: Date = Date(),
)

// ─── User ─────────────────────────────────────────────────────────────────
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: UserRole = UserRole.USER,
    val isGuest: Boolean = false,
    val isEmailVerified: Boolean = false,
    val membershipType: MembershipType = MembershipType.FREE,
    val totalBooksRead: Int = 0,
    val totalReadingMinutes: Long = 0L,
    val totalDownloads: Int = 0,
    val readingStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: Date = Date(),
    val createdAt: Date = Date(),
    val dailyReadingGoalMinutes: Int = 30,
    val weeklyReadingGoalBooks: Int = 1,
    val preferredLanguage: String = "en",
    val notificationsEnabled: Boolean = true,
    val favoriteCategories: List<String> = emptyList(),
)

enum class UserRole { USER, MODERATOR, ADMIN }

enum class MembershipType(val displayName: String) {
    FREE("Free"),
    PREMIUM("Premium"),
    PRO("Pro")
}

// ─── Reading Stats ─────────────────────────────────────────────────────────
data class ReadingStats(
    val totalBooksRead: Int = 0,
    val totalReadingMinutes: Long = 0L,
    val weeklyMinutes: List<Int> = List(7) { 0 },
    val monthlyBooks: List<Int> = List(12) { 0 },
    val readingStreak: Int = 0,
    val longestStreak: Int = 0,
    val favoriteCategory: BookCategory? = null,
    val booksThisWeek: Int = 0,
    val minutesThisWeek: Long = 0L,
    val booksThisMonth: Int = 0,
)

// ─── Achievement ──────────────────────────────────────────────────────────
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Date? = null,
    val progressCurrent: Int = 0,
    val progressTarget: Int = 1,
)

// ─── Book Request ─────────────────────────────────────────────────────────
data class BookRequest(
    val id: String = "",
    val userId: String = "",
    val bookTitle: String = "",
    val authorName: String = "",
    val description: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Date = Date(),
)

enum class RequestStatus { PENDING, APPROVED, REJECTED, FULFILLED }

// ─── Notification ─────────────────────────────────────────────────────────
data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val data: Map<String, String> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Date = Date(),
)

enum class NotificationType { NEW_BOOK, RECOMMENDATION, READING_REMINDER, GENERAL }
