package com.myreader.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "myreader_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModuleFixed {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    /**
     * Provides the application Context directly so repositories
     * like DownloadRepositoryImpl can use it without @ApplicationContext.
     */
    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context = context
}
