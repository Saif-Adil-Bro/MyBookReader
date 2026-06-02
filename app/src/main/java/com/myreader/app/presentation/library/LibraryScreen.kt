package com.myreader.app.presentation.library

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myreader.app.domain.model.Book
import com.myreader.app.domain.model.DownloadedBook
import com.myreader.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onReadClick: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Favorites", "Downloads", "Recent")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) },
                        icon = {
                            Icon(when (idx) {
                                0 -> Icons.Filled.Favorite
                                1 -> Icons.Filled.Download
                                else -> Icons.Filled.History
                            }, null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> FavoritesTab(state.favorites, onBookClick, viewModel::removeFromFavorites)
                1 -> DownloadsTab(state.downloads, onBookClick, onReadClick, viewModel::deleteDownload)
                2 -> RecentTab(state.recentlyRead, onBookClick)
            }
        }
    }
}

@Composable
private fun FavoritesTab(
    books: List<Book>,
    onBookClick: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (books.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.FavoriteBorder,
            title = "No Favorites Yet",
            subtitle = "Books you favorite will appear here",
        )
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(books, key = { it.id }) { book ->
                BookListItem(
                    book = book,
                    onClick = { onBookClick(book.id) },
                    trailingContent = {
                        IconButton(onClick = { onRemove(book.id) }) {
                            Icon(Icons.Filled.Favorite, null, tint = ErrorRed)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DownloadsTab(
    downloads: List<DownloadedBook>,
    onBookClick: (String) -> Unit,
    onReadClick: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (downloads.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.CloudDownload,
            title = "No Downloads",
            subtitle = "Downloaded books will be available here for offline reading",
        )
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(downloads, key = { it.bookId }) { download ->
                DownloadItem(
                    download = download,
                    onClick = { onBookClick(download.bookId) },
                    onRead = { onReadClick(download.bookId, true) },
                    onDelete = { onDelete(download.bookId) },
                )
            }
        }
    }
}

@Composable
private fun RecentTab(books: List<Book>, onBookClick: (String) -> Unit) {
    if (books.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.History,
            title = "No Reading History",
            subtitle = "Books you read will show up here",
        )
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(books, key = { it.id }) { book ->
                BookListItem(book = book, onClick = { onBookClick(book.id) })
            }
        }
    }
}

@Composable
fun BookListItem(
    book: Book,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = null,
                modifier = Modifier.size(60.dp, 85.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(book.author, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = Emerald100, shape = RoundedCornerShape(4.dp)) {
                        Text(book.category.displayNameEn, style = MaterialTheme.typography.labelSmall,
                            color = Emerald700, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = Gold500, modifier = Modifier.size(12.dp))
                        Text(" %.1f".format(book.rating), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            trailingContent?.invoke()
        }
    }
}

@Composable
private fun DownloadItem(
    download: DownloadedBook,
    onClick: () -> Unit,
    onRead: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Format badge
                Surface(color = Emerald100, shape = RoundedCornerShape(4.dp)) {
                    Text(download.format.name, style = MaterialTheme.typography.labelSmall,
                        color = Emerald700, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${(download.fileSizeBytes / 1024 / 1024)} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(download.bookId, style = MaterialTheme.typography.titleSmall, // In real use, show title
                maxLines = 2, overflow = TextOverflow.Ellipsis)

            // Reading progress
            if (download.totalPages > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Emerald600,
                )
                Text(
                    "${(download.progress * 100).toInt()}% • Page ${download.lastReadPage} / ${download.totalPages}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRead,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.ChromeReaderMode, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Read", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(0.5f)),
                ) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = ErrorRed)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Download") },
            text = { Text("Remove this book from downloads? You can download it again later.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(icon, null, modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
