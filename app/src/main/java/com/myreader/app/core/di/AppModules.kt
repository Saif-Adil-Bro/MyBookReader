package com.myreader.app.core.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import com.myreader.app.data.local.database.MyReaderDatabase
import com.myreader.app.data.local.dao.*
import com.myreader.app.data.repository.*
import com.myreader.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ─── Firebase Module ──────────────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().also {
        it.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
    }

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}

// ─── Database Module ──────────────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): MyReaderDatabase =
        Room.databaseBuilder(ctx, MyReaderDatabase::class.java, "myreader.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBookDao(db: MyReaderDatabase): BookDao = db.bookDao()
    @Provides fun provideDownloadDao(db: MyReaderDatabase): DownloadDao = db.downloadDao()
    @Provides fun provideReadingProgressDao(db: MyReaderDatabase): ReadingProgressDao = db.readingProgressDao()
    @Provides fun provideBookmarkDao(db: MyReaderDatabase): BookmarkDao = db.bookmarkDao()
    @Provides fun provideReadingHistoryDao(db: MyReaderDatabase): ReadingHistoryDao = db.readingHistoryDao()
    @Provides fun providePendingSyncDao(db: MyReaderDatabase): PendingSyncDao = db.pendingSyncDao()
}

// ─── Repository Module ────────────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepo(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindBookRepo(impl: BookRepositoryImpl): BookRepository

    @Binds @Singleton
    abstract fun bindDownloadRepo(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds @Singleton
    abstract fun bindReadingRepo(impl: ReadingRepositoryImpl): ReadingRepository

    @Binds @Singleton
    abstract fun bindUserRepo(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindAdminRepo(impl: AdminRepositoryImpl): AdminRepository
}

// ─── DataStore / Preferences ──────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> =
        androidx.datastore.preferences.preferencesDataStore(name = "myreader_prefs").getValue(ctx, String::class.java)
}
