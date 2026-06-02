package com.myreader.app.presentation.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.domain.model.Book
import com.myreader.app.domain.repository.BookRepository
import com.myreader.app.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailState(
    val book: Book? = null,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val shareLink: String = "",
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookRepo: BookRepository,
    private val downloadRepo: DownloadRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    private val _state = MutableStateFlow(BookDetailState())
    val state: StateFlow<BookDetailState> = _state.asStateFlow()

    init {
        loadBook()
        observeStatus()
    }

    private fun loadBook() = viewModelScope.launch {
        bookRepo.getBookById(bookId)
            .onSuccess { book ->
                _state.update { it.copy(book = book, isLoading = false,
                    shareLink = "https://myreader.app/book/$bookId") }
            }
            .onFailure { e -> _state.update { it.copy(error = e.message, isLoading = false) } }
    }

    private fun observeStatus() = viewModelScope.launch {
        combine(
            bookRepo.isFavorite(bookId),
            downloadRepo.getDownloadProgress(bookId),
        ) { fav, progress ->
            _state.update {
                it.copy(isFavorite = fav, downloadProgress = progress,
                    isDownloading = progress in 1..99)
            }
        }.collect()
    }

    fun toggleFavorite() = viewModelScope.launch {
        if (_state.value.isFavorite) bookRepo.removeFromFavorites(bookId)
        else bookRepo.addToFavorites(bookId)
    }

    fun downloadBook() = viewModelScope.launch {
        val book = _state.value.book ?: return@launch
        _state.update { it.copy(isDownloading = true) }
        downloadRepo.downloadBook(book)
            .onSuccess {
                bookRepo.incrementDownloadCount(bookId)
                _state.update { it.copy(isDownloading = false, isDownloaded = true) }
            }
            .onFailure { e -> _state.update { it.copy(isDownloading = false, error = e.message) } }
    }

    fun rateBook(rating: Float) = viewModelScope.launch {
        bookRepo.rateBook(bookId, rating)
    }

    fun reportBroken(reason: String) = viewModelScope.launch {
        bookRepo.reportBrokenBook(bookId, reason)
    }
}
