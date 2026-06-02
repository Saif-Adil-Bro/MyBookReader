package com.myreader.app.presentation.bookdetail

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myreader.app.domain.model.BookFormat
import com.myreader.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    onReadClick: (String) -> Unit,
    onDownloadRead: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            "Favorite",
                            tint = if (state.isFavorite) Color.Red else LocalContentColor.current,
                        )
                    }
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Outlined.Share, "Share")
                    }
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Outlined.Flag, "Report")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            state.book?.let { book ->
                BookDetailBottomBar(
                    bookId = bookId,
                    isDownloaded = state.isDownloaded,
                    isDownloading = state.isDownloading,
                    downloadProgress = state.downloadProgress,
                    onReadOnline  = { onReadClick(bookId) },
                    onDownload    = { viewModel.downloadBook() },
                    onReadOffline = { onDownloadRead(bookId) },
                )
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            state.book?.let { book ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = padding.calculateBottomPadding())
                ) {
                    // Hero Section
                    Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                        // Blurred bg
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(
                                Color.Black.copy(0.4f), Color.Black.copy(0.8f)
                            ))
                        ))
                        // Cover + Info Row
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            // Book cover
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = book.title,
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(155.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(book.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(book.author,
                                    style = MaterialTheme.typography.bodyMedium, color = White.copy(0.8f))
                                Spacer(Modifier.height(8.dp))
                                // Category chip
                                Surface(
                                    color = Emerald600.copy(0.85f),
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        book.category.displayNameEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatItem(label = "Rating", value = "%.1f".format(book.rating), icon = Icons.Filled.Star, iconTint = Gold500)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        StatItem(label = "Downloads", value = formatCount(book.downloadCount), icon = Icons.Filled.Download)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        StatItem(label = "Pages", value = "${book.totalPages}", icon = Icons.Filled.MenuBook)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        StatItem(label = "Size", value = book.fileSizeMb, icon = Icons.Filled.Storage)
                    }

                    HorizontalDivider()

                    // Description
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("About this book", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        ExpandableText(text = book.description)
                    }

                    // Details
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Book Details", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            DetailRow("Format", book.fileFormat.name)
                            DetailRow("Language", book.language.displayName)
                            if (book.publishedYear > 0) DetailRow("Published", "${book.publishedYear}")
                            if (book.isbn.isNotBlank()) DetailRow("ISBN", book.isbn)
                            if (book.publisher.isNotBlank()) DetailRow("Publisher", book.publisher)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Rate button
                    OutlinedButton(
                        onClick = { showRatingDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Icon(Icons.Outlined.Star, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Rate this Book")
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color = MaterialTheme.colorScheme.primary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ExpandableText(text: String, maxLines: Int = 4) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else maxLines,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
        )
        if (text.length > 200) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(if (expanded) "Show less" else "Read more")
            }
        }
    }
}

@Composable
private fun BookDetailBottomBar(
    bookId: String,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    onReadOnline: () -> Unit,
    onDownload: () -> Unit,
    onReadOffline: () -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onReadOnline,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.ChromeReaderMode, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Read Now")
            }

            when {
                isDownloaded -> {
                    OutlinedButton(
                        onClick = onReadOffline,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.DownloadDone, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Read Offline")
                    }
                }
                isDownloading -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("$downloadProgress%")
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download")
                    }
                }
            }
        }
    }
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000     -> "%.1fK".format(count / 1_000.0)
    else               -> count.toString()
}
