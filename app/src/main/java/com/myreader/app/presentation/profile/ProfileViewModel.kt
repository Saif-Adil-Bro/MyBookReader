package com.myreader.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.core.utils.PreferencesManager
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val stats: ReadingStats = ReadingStats(),
    val achievements: List<Achievement> = emptyList(),
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    private val prefs: PreferencesManager,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepo.currentUser,
        userRepo.getReadingStats(),
        userRepo.getAchievements(),
        prefs.isDarkTheme,
    ) { user, stats, achievements, dark ->
        ProfileUiState(user = user, stats = stats, achievements = achievements,
            isDarkMode = dark, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun toggleDarkMode() = viewModelScope.launch {
        prefs.setDarkTheme(!uiState.value.isDarkMode)
    }

    fun signOut() = viewModelScope.launch { authRepo.signOut() }
}
