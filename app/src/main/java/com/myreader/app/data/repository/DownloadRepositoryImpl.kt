package com.myreader.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myreader.app.data.local.dao.DownloadDao
import com.myreader.app.data.local.entity.DownloadedBookEntity
import com.myreader.app.domain.model.Book
import com.myreader.app.domain.model.BookFormat
import com.myreader.app.domain.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val context: Context,
    private val workManager: WorkManager,
) : DownloadRepository {

    override fun getDownloadedBooks(): Flow<List<com.myreader.app.domain.model.DownloadedBook>> =
        downloadDao.getAllDownloads().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun downloadBook(book: Book): Result<Unit> = runCatching {
        val request = OneTimeWorkRequestBuilder<BookDownloadWorker>()
            .setInputData(
                workDataOf(
                    BookDownloadWorker.KEY_BOOK_ID    to book.id,
                    BookDownloadWorker.KEY_BOOK_TITLE to book.title,
                    BookDownloadWorker.KEY_FILE_URL   to book.fileUrl,
                    BookDownloadWorker.KEY_FORMAT     to book.fileFormat.name,
                    BookDownloadWorker.KEY_COVER_URL  to book.coverUrl,
                    BookDownloadWorker.KEY_AUTHOR     to book.author,
                    BookDownloadWorker.KEY_CATEGORY   to book.category.name,
                )
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .addTag("download_${book.id}")
            .build()
        workManager.enqueueUniqueWork(
            "download_${book.id}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun deleteDownload(bookId: String): Result<Unit> = runCatching {
        val entity = downloadDao.getDownload(bookId) ?: return@runCatching
        File(entity.localFilePath).delete()
        downloadDao.deleteDownload(entity)
    }

    override suspend fun isBookDownloaded(bookId: String): Boolean =
        downloadDao.isDownloaded(bookId)

    override fun getDownloadProgress(bookId: String): Flow<Int> =
        workManager.getWorkInfosByTagLiveData("download_$bookId")
            .asFlow()
            .map { list ->
                list.firstOrNull()?.progress?.getInt(BookDownloadWorker.KEY_PROGRESS, 0) ?: 0
            }

    override suspend fun getLocalFilePath(bookId: String): String? =
        downloadDao.getDownload(bookId)?.localFilePath

    private fun DownloadedBookEntity.toDomain() = com.myreader.app.domain.model.DownloadedBook(
        bookId = bookId,
        localFilePath = localFilePath,
        downloadedAt = downloadedAt,
        fileSizeBytes = fileSizeBytes,
        format = BookFormat.valueOf(format),
        lastReadPage = lastReadPage,
        totalPages = totalPages,
        lastOpenedAt = lastOpenedAt,
    )
}

// ─── WorkManager Worker ───────────────────────────────────────────────────
@HiltWorker
class BookDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_BOOK_ID    = "book_id"
        const val KEY_BOOK_TITLE = "book_title"
        const val KEY_FILE_URL   = "file_url"
        const val KEY_FORMAT     = "format"
        const val KEY_COVER_URL  = "cover_url"
        const val KEY_AUTHOR     = "author"
        const val KEY_CATEGORY   = "category"
        const val KEY_PROGRESS   = "progress"
    }

    override suspend fun doWork(): Result {
        val bookId   = inputData.getString(KEY_BOOK_ID)   ?: return Result.failure()
        val fileUrl  = inputData.getString(KEY_FILE_URL)  ?: return Result.failure()
        val format   = inputData.getString(KEY_FORMAT)    ?: "PDF"
        val title    = inputData.getString(KEY_BOOK_TITLE) ?: bookId
        val author   = inputData.getString(KEY_AUTHOR)    ?: ""
        val coverUrl = inputData.getString(KEY_COVER_URL) ?: ""
        val category = inputData.getString(KEY_CATEGORY)  ?: ""

        return try {
            val dir = File(applicationContext.filesDir, "books").also { it.mkdirs() }
            val ext = if (format == "EPUB") "epub" else "pdf"
            val file = File(dir, "$bookId.$ext")

            // Download with progress
            val request = Request.Builder().url(fileUrl).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body ?: return Result.failure()
            val contentLength = body.contentLength()

            FileOutputStream(file).use { fos ->
                body.byteStream().use { bis ->
                    var bytesRead = 0L
                    val buffer = ByteArray(8192)
                    var n: Int
                    while (bis.read(buffer).also { n = it } != -1) {
                        fos.write(buffer, 0, n)
                        bytesRead += n
                        if (contentLength > 0) {
                            val progress = ((bytesRead.toFloat() / contentLength) * 100).toInt()
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                }
            }

            // Save to DB
            downloadDao.upsertDownload(
                DownloadedBookEntity(
                    bookId = bookId,
                    localFilePath = file.absolutePath,
                    downloadedAt = Date(),
                    fileSizeBytes = file.length(),
                    format = format,
                    title = title,
                    author = author,
                    coverUrl = coverUrl,
                    category = category,
                )
            )
            setProgress(workDataOf(KEY_PROGRESS to 100))
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Download failed for $bookId")
            Result.retry()
        }
    }
}

// ─── Helper extension ─────────────────────────────────────────────────────
private fun <T> androidx.lifecycle.LiveData<T>.asFlow(): Flow<T> = callbackFlow {
    val observer = androidx.lifecycle.Observer<T> { trySend(it) }
    observeForever(observer)
    awaitClose { removeObserver(observer) }
}
