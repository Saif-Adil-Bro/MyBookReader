package com.myreader.app.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.myreader.app.presentation.theme.*

// ─── ViewModel ────────────────────────────────────────────────────────────
@HiltViewModel
class UsersViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
) : ViewModel() {

    val users: StateFlow<List<User>> = adminRepo.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredUsers: StateFlow<List<User>> = combine(users, searchQuery) { userList, q ->
        if (q.isBlank()) userList
        else userList.filter {
            it.displayName.contains(q, ignoreCase = true) ||
            it.email.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearch(query: String) { _searchQuery.value = query }

    fun banUser(userId: String) = viewModelScope.launch {
        adminRepo.banUser(userId)
    }

    fun promoteToModerator(userId: String) = viewModelScope.launch {
        // Update via Firestore
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementScreen(
    onBack: () -> Unit,
    viewModel: UsersViewModel = hiltViewModel(),
) {
    val users by viewModel.filteredUsers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var banTargetId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearch,
                placeholder = { Text("Search users by name or email...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearch("") }) { Icon(Icons.Filled.Clear, null) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            // Stats header
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald50),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniStat("${users.size}", "Total")
                    VerticalDivider(modifier = Modifier.height(30.dp))
                    MiniStat("${users.count { it.role == UserRole.ADMIN }}", "Admins")
                    VerticalDivider(modifier = Modifier.height(30.dp))
                    MiniStat("${users.count { !it.isGuest }}", "Registered")
                    VerticalDivider(modifier = Modifier.height(30.dp))
                    MiniStat("${users.count { it.isGuest }}", "Guests")
                }
            }

            if (users.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.PeopleOutline, null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("No users found", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(users, key = { it.uid }) { user ->
                        UserCard(
                            user = user,
                            onBan = { banTargetId = user.uid },
                        )
                    }
                }
            }
        }
    }

    // Ban confirmation
    banTargetId?.let { uid ->
        AlertDialog(
            onDismissRequest = { banTargetId = null },
            icon = { Icon(Icons.Filled.Block, null, tint = ErrorRed) },
            title = { Text("Ban User") },
            text = { Text("This user will lose access to the app. This action can be reversed from Firebase Console.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.banUser(uid); banTargetId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                ) { Text("Ban User") }
            },
            dismissButton = { TextButton(onClick = { banTargetId = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun UserCard(user: User, onBan: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .then(
                        if (user.photoUrl.isNotBlank())
                            Modifier
                        else Modifier.background(Emerald100)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (user.photoUrl.isNotBlank()) {
                    AsyncImage(model = user.photoUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(
                        user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = Emerald700,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.displayName, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    // Role badge
                    if (user.role != UserRole.USER) {
                        Surface(
                            color = if (user.role == UserRole.ADMIN) ErrorRed.copy(0.15f) else Emerald100,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                user.role.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.role == UserRole.ADMIN) ErrorRed else Emerald700,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (user.isGuest) {
                        Spacer(Modifier.width(4.dp))
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                            Text("Guest", style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(user.email.ifBlank { "No email (guest)" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📚 ${user.totalBooksRead}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("🔥 ${user.readingStreak}d", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Actions
            if (user.role != UserRole.ADMIN) {
                IconButton(onClick = onBan) {
                    Icon(Icons.Outlined.Block, "Ban user",
                        tint = ErrorRed.copy(0.7f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Emerald700)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val Emerald50  = Color(0xFFECFDF5)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald700 = Color(0xFF047857)

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
