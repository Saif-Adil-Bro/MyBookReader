package com.myreader.app.presentation.admin

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.myreader.app.presentation.theme.*

// ─── ViewModel ────────────────────────────────────────────────────────────
data class AdminUiState(
    val stats: AdminStats = AdminStats(),
    val recentBooks: List<Book> = emptyList(),
    val bookRequests: List<BookRequest> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
    private val bookRepo: BookRepository,
) : ViewModel() {

    val uiState: StateFlow<AdminUiState> = combine(
        adminRepo.getAdminStats(),
        bookRepo.getNewlyAddedBooks(20),
        adminRepo.getBookRequests(),
    ) { stats, books, requests ->
        AdminUiState(stats = stats, recentBooks = books, bookRequests = requests, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminUiState())

    fun deleteBook(bookId: String) = viewModelScope.launch {
        adminRepo.deleteBook(bookId)
            .onSuccess { /* refresh */ }
    }

    fun approveRequest(requestId: String) = viewModelScope.launch {
        adminRepo.updateBookRequestStatus(requestId, RequestStatus.APPROVED)
    }

    fun rejectRequest(requestId: String) = viewModelScope.launch {
        adminRepo.updateBookRequestStatus(requestId, RequestStatus.REJECTED)
    }

    fun sendNotification(title: String, body: String) = viewModelScope.launch {
        adminRepo.sendPushNotification(title, body, null)
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onAddBook: () -> Unit,
    onEditBook: (String) -> Unit,
    onUsersClick: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Books", "Requests", "Notify")
    var showNotifDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onUsersClick) { Icon(Icons.Outlined.People, "Users") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Emerald700,
                    titleContentColor = White,
                    navigationIconContentColor = White,
                    actionIconContentColor = White,
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = onAddBook, containerColor = Emerald600) {
                    Icon(Icons.Filled.Add, "Add Book", tint = White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                }
            }

            when (selectedTab) {
                0 -> AdminOverview(stats = state.stats, onUsersClick = onUsersClick)
                1 -> AdminBooksList(books = state.recentBooks, onEdit = onEditBook, onDelete = viewModel::deleteBook)
                2 -> AdminRequests(
                    requests = state.bookRequests,
                    onApprove = viewModel::approveRequest,
                    onReject = viewModel::rejectRequest,
                )
                3 -> AdminNotifications(onSend = { title, body ->
                    viewModel.sendNotification(title, body)
                    showNotifDialog = false
                })
            }
        }
    }
}

@Composable
private fun AdminOverview(stats: AdminStats, onUsersClick: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Overview", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard(Modifier.weight(1f), Icons.Filled.Book, "${stats.totalBooks}", "Total Books", Emerald600)
                AdminStatCard(Modifier.weight(1f), Icons.Filled.People, "${stats.totalUsers}", "Total Users", InfoBlue)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard(Modifier.weight(1f), Icons.Filled.Download, "${stats.totalDownloads}", "Downloads", Gold600)
                AdminStatCard(Modifier.weight(1f), Icons.Filled.PersonAdd, "${stats.newUsersToday}", "New Today", SuccessGreen)
            }
        }

        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onUsersClick).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.SupervisedUserCircle, null, tint = Emerald600, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage Users", style = MaterialTheme.typography.titleSmall)
                        Text("${stats.activeUsersToday} active today", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }
        }

        if (stats.popularCategories.isNotEmpty()) {
            item {
                Text("Popular Categories", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                stats.popularCategories.forEach { (cat, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(cat.displayNameEn, modifier = Modifier.width(140.dp),
                            style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = { count.toFloat() / (stats.popularCategories.maxOfOrNull { it.second } ?: 1) },
                            modifier = Modifier.weight(1f),
                            color = Emerald600,
                        )
                        Text(" $count", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminBooksList(books: List<Book>, onEdit: (String) -> Unit, onDelete: (String) -> Unit) {
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(books, key = { it.id }) { book ->
            Card(shape = RoundedCornerShape(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        Text(book.author, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${book.downloadCount} downloads • ${book.fileFormat.name}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onEdit(book.id) }) {
                        Icon(Icons.Outlined.Edit, "Edit", tint = Emerald600)
                    }
                    IconButton(onClick = { deleteTarget = book.id }) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = ErrorRed)
                    }
                }
            }
        }
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Book") },
            text = { Text("This action cannot be undone. The book file will also be removed.") },
            confirmButton = {
                TextButton(onClick = { onDelete(id); deleteTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminRequests(
    requests: List<BookRequest>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending book requests", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(requests, key = { it.id }) { req ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(req.bookTitle, style = MaterialTheme.typography.titleSmall)
                                if (req.authorName.isNotBlank())
                                    Text("by ${req.authorName}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RequestStatusChip(req.status)
                        }
                        if (req.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(req.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        }
                        if (req.status == RequestStatus.PENDING) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApprove(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(8.dp),
                                ) { Text("Approve", style = MaterialTheme.typography.labelMedium) }
                                OutlinedButton(
                                    onClick = { onReject(req.id) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(8.dp),
                                    border = BorderStroke(1.dp, ErrorRed),
                                ) { Text("Reject", style = MaterialTheme.typography.labelMedium, color = ErrorRed) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestStatusChip(status: RequestStatus) {
    val (color, label) = when (status) {
        RequestStatus.PENDING   -> Pair(WarningAmber, "Pending")
        RequestStatus.APPROVED  -> Pair(SuccessGreen, "Approved")
        RequestStatus.REJECTED  -> Pair(ErrorRed, "Rejected")
        RequestStatus.FULFILLED -> Pair(Emerald600, "Done")
    }
    Surface(color = color.copy(0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun AdminNotifications(onSend: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Send Push Notification", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Notification Title") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = body, onValueChange = { body = it },
            label = { Text("Message Body") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 5,
        )
        Spacer(Modifier.height(16.dp))

        // Quick templates
        Text("Quick Templates", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        listOf(
            "📚 New Book Added!" to "Check out the latest addition to our library!",
            "⏰ Reading Reminder" to "Don't forget your daily reading goal. Keep your streak going!",
            "🏆 Weekly Challenge" to "New weekly reading challenge is live. Can you complete it?",
        ).forEach { (t, b) ->
            Surface(
                onClick = { title = t; body = b },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(t, style = MaterialTheme.typography.labelMedium)
                    Text(b, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { if (title.isNotBlank() && body.isNotBlank()) onSend(title, body) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = title.isNotBlank() && body.isNotBlank(),
        ) {
            Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Send to All Users")
        }
    }
}
