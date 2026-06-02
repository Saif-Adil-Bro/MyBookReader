package com.myreader.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────
data class LibraryUiState(
    val favorites: List<Book> = emptyList(),
    val downloads: List<DownloadedBook> = emptyList(),
    val recentlyRead: List<Book> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepo: BookRepository,
    private val downloadRepo: DownloadRepository,
    private val readingRepo: ReadingRepository,
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        bookRepo.getFavoriteBooks(),
        downloadRepo.getDownloadedBooks(),
        readingRepo.getRecentlyRead(),
    ) { favorites, downloads, recent ->
        LibraryUiState(
            favorites = favorites,
            downloads = downloads,
            recentlyRead = recent,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun deleteDownload(bookId: String) = viewModelScope.launch {
        downloadRepo.deleteDownload(bookId)
    }

    fun removeFromFavorites(bookId: String) = viewModelScope.launch {
        bookRepo.removeFromFavorites(bookId)
    }
}
