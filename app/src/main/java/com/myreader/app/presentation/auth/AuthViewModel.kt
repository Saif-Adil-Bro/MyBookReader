package com.myreader.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val emailSent: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithEmail(email, password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) } }
        }
    }

    fun signUp(email: String, password: String, name: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signUpWithEmail(email, password, name)
                .onSuccess { _uiState.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) } }
        }
    }

    fun signInWithGoogle(idToken: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.signInWithGoogle(idToken)
            .onSuccess { _uiState.update { it.copy(isLoading = false, success = true) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) } }
    }

    fun continueAsGuest() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.signInAsGuest()
            .onSuccess { _uiState.update { it.copy(isLoading = false, success = true) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) } }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.sendPasswordResetEmail(email)
                .onSuccess { _uiState.update { it.copy(isLoading = false, emailSent = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
