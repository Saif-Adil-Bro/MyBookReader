package com.myreader.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featuredBooks: List<Book> = emptyList(),
    val popularBooks: List<Book> = emptyList(),
    val newlyAdded: List<Book> = emptyList(),
    val topRated: List<Book> = emptyList(),
    val continueReading: List<Book> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookRepo: BookRepository,
    private val authRepo: AuthRepository,
    private val readingRepo: ReadingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() = viewModelScope.launch {
        combine(
            authRepo.currentUser,
            bookRepo.getFeaturedBooks(),
            bookRepo.getPopularBooks(),
            bookRepo.getNewlyAddedBooks(),
            bookRepo.getTopRatedBooks(),
        ) { user, featured, popular, newly, topRated ->
            HomeUiState(
                currentUser  = user,
                featuredBooks = featured,
                popularBooks  = popular,
                newlyAdded    = newly,
                topRated      = topRated,
                isLoading     = false,
            )
        }.catch { e ->
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }.collect { state ->
            _uiState.update { state }
        }
    }

    fun refreshData() = loadData()
}
