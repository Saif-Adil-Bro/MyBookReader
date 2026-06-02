package com.myreader.app.presentation.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myreader.app.core.utils.PreferencesManager
import com.myreader.app.presentation.theme.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Onboarding ───────────────────────────────────────────────────────────
data class OnboardingPage(val emoji: String, val title: String, val subtitle: String)

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val prefs: PreferencesManager) : ViewModel() {
    fun markSeen() = viewModelScope.launch { prefs.setSeenOnboarding() }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val pages = listOf(
        OnboardingPage("📚", "Your Digital Library", "Browse thousands of books across multiple categories in one place."),
        OnboardingPage("📖", "Read Anywhere", "Download books for offline reading. Pick up exactly where you left off."),
        OnboardingPage("🔥", "Build Your Habit", "Track your reading streak, set goals, and earn achievement badges."),
    )
    var current by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Emerald900, Emerald700))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(40.dp))

            // Page content
            val page = pages[current]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(page.emoji, style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(32.dp))
                Text(page.title, style = MaterialTheme.typography.headlineMedium,
                    color = White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(page.subtitle, style = MaterialTheme.typography.bodyLarge,
                    color = White.copy(0.8f), textAlign = TextAlign.Center)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (current == idx) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(if (current == idx) Gold400 else White.copy(0.4f))
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (current < pages.lastIndex) current++
                        else { viewModel.markSeen(); onComplete() }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                ) {
                    Text(if (current < pages.lastIndex) "Next" else "Get Started",
                        style = MaterialTheme.typography.titleMedium, color = Emerald900)
                }
                if (current < pages.lastIndex) {
                    TextButton(onClick = { viewModel.markSeen(); onComplete() }) {
                        Text("Skip", color = White.copy(0.7f))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─── Register ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.success) { if (state.success) onRegisterSuccess() }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Create Account") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Full Name") }, leadingIcon = { Icon(Icons.Outlined.Person, null) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email Address") }, leadingIcon = { Icon(Icons.Outlined.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                    }
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

            state.error?.let { ErrorCard(it, viewModel::clearError) }

            Button(
                onClick = { viewModel.signUp(email, password, name, confirmPassword) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("Create Account", style = MaterialTheme.typography.titleMedium)
            }

            Text(
                "By registering, you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── Forgot Password ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Reset Password") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Outlined.LockReset, null, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally),
                tint = Emerald600)
            Text("Enter your email address and we'll send you a link to reset your password.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email Address") }, leadingIcon = { Icon(Icons.Outlined.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

            if (state.emailSent) {
                Card(colors = CardDefaults.cardColors(containerColor = Emerald100), shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Emerald700, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reset email sent! Check your inbox.", color = Emerald700,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            state.error?.let { ErrorCard(it, viewModel::clearError) }

            Button(
                onClick = { viewModel.resetPassword(email) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading && !state.emailSent,
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("Send Reset Email")
            }
        }
    }
}
