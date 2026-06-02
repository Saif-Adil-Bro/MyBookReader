package com.myreader.app.presentation.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.core.utils.PreferencesManager
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.*
import com.myreader.app.presentation.theme.ReaderTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class ReaderUiState(
    val book: Book? = null,
    val localFilePath: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val bookmarks: List<Bookmark> = emptyList(),
    val readerTheme: ReaderTheme = ReaderTheme.LIGHT,
    val fontSize: Float = 16f,
    val lineHeight: Float = 1.6f,
    val showControls: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showBookmarkPanel: Boolean = false,
    val showFontPanel: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Int> = emptyList(), // page numbers
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookRepo: BookRepository,
    private val readingRepo: ReadingRepository,
    private val downloadRepo: DownloadRepository,
    private val prefs: PreferencesManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    private val isDownloaded: Boolean = savedStateHandle["isDownloaded"] ?: false

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var readingTimer: Job? = null
    private var readingSeconds = 0L

    init {
        loadBook()
        observePreferences()
        startReadingTimer()
    }

    private fun loadBook() = viewModelScope.launch {
        try {
            val book = bookRepo.getBookById(bookId).getOrThrow()
            val filePath = if (isDownloaded) downloadRepo.getLocalFilePath(bookId) else book.fileUrl
            val progress = readingRepo.getReadingProgress(bookId).firstOrNull()

            _state.update {
                it.copy(
                    book = book,
                    localFilePath = filePath,
                    currentPage = progress?.currentPage ?: 0,
                    totalPages = progress?.totalPages ?: book.totalPages,
                    isLoading = false,
                )
            }

            // Load bookmarks
            readingRepo.getBookmarks(bookId).collect { bms ->
                _state.update { it.copy(bookmarks = bms) }
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    private fun observePreferences() = viewModelScope.launch {
        combine(prefs.readerTheme, prefs.readerFontSize, prefs.readerLineHeight) { theme, size, height ->
            Triple(theme, size, height)
        }.collect { (theme, size, height) ->
            _state.update {
                it.copy(
                    readerTheme = ReaderTheme.valueOf(theme),
                    fontSize = size,
                    lineHeight = height,
                )
            }
        }
    }

    private fun startReadingTimer() {
        readingTimer = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                readingSeconds++
            }
        }
    }

    fun onPageChanged(page: Int, total: Int) {
        _state.update { it.copy(currentPage = page, totalPages = total) }
        // Debounced save
        viewModelScope.launch {
            delay(500)
            saveProgress(page, total)
        }
    }

    fun toggleControls() = _state.update { it.copy(showControls = !it.showControls) }

    fun setReaderTheme(theme: ReaderTheme) = viewModelScope.launch {
        prefs.setReaderTheme(theme.name)
    }

    fun setFontSize(size: Float) = viewModelScope.launch {
        val clamped = size.coerceIn(10f, 30f)
        prefs.setReaderFontSize(clamped)
    }

    fun setLineHeight(height: Float) = viewModelScope.launch {
        val clamped = height.coerceIn(1.0f, 2.5f)
        prefs.setReaderLineHeight(clamped)
    }

    fun addBookmark(label: String = "", note: String = "") = viewModelScope.launch {
        val bookmark = Bookmark(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            page = _state.value.currentPage,
            label = label.ifBlank { "Page ${_state.value.currentPage + 1}" },
            note = note,
            createdAt = Date(),
        )
        readingRepo.addBookmark(bookmark)
    }

    fun deleteBookmark(bookmarkId: String) = viewModelScope.launch {
        readingRepo.deleteBookmark(bookmarkId)
    }

    fun goToPage(page: Int) {
        _state.update { it.copy(currentPage = page) }
    }

    fun searchInBook(query: String) {
        _state.update { it.copy(searchQuery = query) }
        // In real implementation, delegate to PDF/EPUB reader native search
    }

    fun toggleBookmarkPanel() = _state.update { it.copy(showBookmarkPanel = !it.showBookmarkPanel) }
    fun toggleFontPanel() = _state.update { it.copy(showFontPanel = !it.showFontPanel) }

    private suspend fun saveProgress(page: Int, total: Int) {
        val progress = ReadingProgress(
            bookId = bookId,
            userId = "",
            currentPage = page,
            totalPages = total,
            lastReadAt = Date(),
            totalReadingMinutes = readingSeconds / 60,
        )
        readingRepo.saveReadingProgress(progress)
    }

    override fun onCleared() {
        super.onCleared()
        readingTimer?.cancel()
        // Save final progress
        viewModelScope.launch {
            val s = _state.value
            saveProgress(s.currentPage, s.totalPages)
        }
    }
}
