package com.myreader.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.core.utils.PreferencesManager
import com.myreader.app.domain.repository.AuthRepository
import com.myreader.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: PreferencesManager,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isDarkTheme: StateFlow<Boolean> = prefs.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val startDestination: StateFlow<String?> = combine(
        authRepository.currentUser,
        prefs.hasSeenOnboarding,
    ) { user, hasSeenOnboarding ->
        when {
            !hasSeenOnboarding -> Screen.Onboarding.route
            user == null       -> Screen.Login.route
            else               -> Screen.Home.route
        }
    }.onEach { _isLoading.value = false }
     .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
