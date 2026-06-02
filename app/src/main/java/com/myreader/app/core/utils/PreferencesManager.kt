package com.myreader.app.core.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("myreader_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val DARK_THEME         = booleanPreferencesKey("dark_theme")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("seen_onboarding")
        val READER_THEME       = stringPreferencesKey("reader_theme")
        val READER_FONT_SIZE   = floatPreferencesKey("reader_font_size")
        val READER_LINE_HEIGHT = floatPreferencesKey("reader_line_height")
        val READER_FONT_FAMILY = stringPreferencesKey("reader_font_family")
        val LANGUAGE           = stringPreferencesKey("language")
        val DAILY_GOAL_MINS    = intPreferencesKey("daily_goal_minutes")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: false }
    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_ONBOARDING] ?: false }
    val readerTheme: Flow<String> = context.dataStore.data.map { it[READER_THEME] ?: "LIGHT" }
    val readerFontSize: Flow<Float> = context.dataStore.data.map { it[READER_FONT_SIZE] ?: 16f }
    val readerLineHeight: Flow<Float> = context.dataStore.data.map { it[READER_LINE_HEIGHT] ?: 1.6f }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "en" }

    suspend fun setDarkTheme(enabled: Boolean) = context.dataStore.edit { it[DARK_THEME] = enabled }
    suspend fun setSeenOnboarding() = context.dataStore.edit { it[HAS_SEEN_ONBOARDING] = true }
    suspend fun setReaderTheme(theme: String) = context.dataStore.edit { it[READER_THEME] = theme }
    suspend fun setReaderFontSize(size: Float) = context.dataStore.edit { it[READER_FONT_SIZE] = size }
    suspend fun setReaderLineHeight(h: Float) = context.dataStore.edit { it[READER_LINE_HEIGHT] = h }
    suspend fun setLanguage(lang: String) = context.dataStore.edit { it[LANGUAGE] = lang }
}
