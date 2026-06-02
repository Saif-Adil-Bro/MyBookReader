package com.myreader.app.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myreader.app.data.local.dao.ReadingHistoryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ── Streak Checker – runs daily at midnight ────────────────────────────────
@HiltWorker
class StreakWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val historyDao: ReadingHistoryDao,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success()

        try {
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }.timeInMillis

            // Check if user read something yesterday
            val readYesterday = historyDao.getTotalMinutesSince(uid, yesterday) ?: 0L
            val userRef = firestore.collection("users").document(uid)
            val userDoc = userRef.get().await()
            val currentStreak = userDoc.getLong("readingStreak") ?: 0L
            val longestStreak = userDoc.getLong("longestStreak") ?: 0L

            if (readYesterday > 0) {
                val newStreak = currentStreak + 1
                userRef.update(
                    mapOf(
                        "readingStreak" to newStreak,
                        "longestStreak" to maxOf(longestStreak, newStreak),
                    )
                ).await()
                Timber.d("Streak updated to $newStreak")
            } else {
                // Reset streak
                userRef.update("readingStreak", 0).await()
                Timber.d("Streak reset to 0")
            }
        } catch (e: Exception) {
            Timber.e(e, "StreakWorker failed")
            return Result.retry()
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
            }
            val delay = midnight.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<StreakWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "streak_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}

// ── Sync Worker – syncs pending local changes to Firestore ─────────────────
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val pendingSyncDao: com.myreader.app.data.local.dao.PendingSyncDao,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (auth.currentUser == null) return Result.success()

        return try {
            val pending = pendingSyncDao.getPending()
            Timber.d("Syncing ${pending.size} pending items")

            val processedIds = mutableListOf<Long>()
            for (item in pending) {
                try {
                    when (item.type) {
                        "PROGRESS" -> syncProgress(item.payload)
                        "BOOKMARK" -> syncBookmark(item.payload)
                        "FAVORITE" -> syncFavorite(item.payload)
                    }
                    processedIds.add(item.id)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync item ${item.id}")
                }
            }
            if (processedIds.isNotEmpty()) {
                pendingSyncDao.deleteByIds(processedIds)
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker failed")
            Result.retry()
        }
    }

    private suspend fun syncProgress(payload: String) {
        val uid = auth.currentUser?.uid ?: return
        // Parse JSON payload and sync to Firestore
        Timber.d("Syncing progress: $payload")
    }

    private suspend fun syncBookmark(payload: String) {
        Timber.d("Syncing bookmark: $payload")
    }

    private suspend fun syncFavorite(payload: String) {
        Timber.d("Syncing favorite: $payload")
    }

    companion object {
        fun scheduleOnConnected(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "data_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
