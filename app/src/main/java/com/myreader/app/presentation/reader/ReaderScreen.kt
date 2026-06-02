package com.myreader.app.presentation.reader

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.*
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.myreader.app.domain.model.BookFormat
import com.myreader.app.domain.model.Bookmark
import com.myreader.app.presentation.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    isDownloaded: Boolean,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    ReaderTheme(readerTheme = state.readerTheme) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            when {
                state.isLoading -> ReaderLoading()
                state.error != null -> ReaderError(state.error!!, onBack)
                else -> {
                    // Main reading area
                    state.book?.let { book ->
                        when (book.fileFormat) {
                            BookFormat.PDF -> PdfReaderView(
                                filePath = state.localFilePath ?: book.fileUrl,
                                startPage = state.currentPage,
                                isLocal = isDownloaded,
                                onPageChanged = viewModel::onPageChanged,
                                onTap = viewModel::toggleControls,
                            )
                            BookFormat.EPUB -> EpubReaderView(
                                fileUrl = state.localFilePath ?: book.fileUrl,
                                fontSize = state.fontSize,
                                lineHeight = state.lineHeight,
                                theme = state.readerTheme,
                                startPage = state.currentPage,
                                onPageChanged = viewModel::onPageChanged,
                                onTap = viewModel::toggleControls,
                            )
                        }
                    }

                    // Overlay controls
                    AnimatedVisibility(
                        visible = state.showControls,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                    ) {
                        ReaderTopBar(
                            title = state.book?.title ?: "",
                            onBack = onBack,
                            onBookmark = { viewModel.addBookmark() },
                            onBookmarkPanel = viewModel::toggleBookmarkPanel,
                            onFontPanel = viewModel::toggleFontPanel,
                        )
                    }

                    // Bottom progress bar
                    AnimatedVisibility(
                        visible = state.showControls,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        ReaderBottomBar(
                            current = state.currentPage + 1,
                            total = state.totalPages,
                            onPageJump = { viewModel.goToPage(it - 1) },
                            onThemeLight = { viewModel.setReaderTheme(ReaderTheme.LIGHT) },
                            onThemeDark = { viewModel.setReaderTheme(ReaderTheme.DARK) },
                            onThemeSepia = { viewModel.setReaderTheme(ReaderTheme.SEPIA) },
                            currentTheme = state.readerTheme,
                        )
                    }

                    // Bookmark Side Panel
                    if (state.showBookmarkPanel) {
                        BookmarkPanel(
                            bookmarks = state.bookmarks,
                            onGoToBookmark = { viewModel.goToPage(it.page) },
                            onDeleteBookmark = { viewModel.deleteBookmark(it.id) },
                            onClose = viewModel::toggleBookmarkPanel,
                        )
                    }

                    // Font Panel
                    if (state.showFontPanel) {
                        FontPanel(
                            fontSize = state.fontSize,
                            lineHeight = state.lineHeight,
                            onFontSizeChange = viewModel::setFontSize,
                            onLineHeightChange = viewModel::setLineHeight,
                            onClose = viewModel::toggleFontPanel,
                        )
                    }
                }
            }
        }
    }
}

// ─── PDF Viewer using AndroidView ─────────────────────────────────────────
@Composable
private fun PdfReaderView(
    filePath: String,
    startPage: Int,
    isLocal: Boolean,
    onPageChanged: (Int, Int) -> Unit,
    onTap: () -> Unit,
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            PDFView(ctx, null).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { pdfView ->
            val configurator = if (isLocal) {
                pdfView.fromFile(File(filePath))
            } else {
                // For online: stream from URI
                pdfView.fromUri(android.net.Uri.parse(filePath))
            }
            configurator
                .defaultPage(startPage)
                .onPageChange { page, pageCount -> onPageChanged(page, pageCount) }
                .onTap { onTap(); true }
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .enableAnnotationRendering(true)
                .pageFitPolicy(FitPolicy.WIDTH)
                .nightMode(false)
                .load()
        },
        modifier = Modifier.fillMaxSize(),
    )
}

// ─── EPUB Viewer using WebView ────────────────────────────────────────────
@Composable
private fun EpubReaderView(
    fileUrl: String,
    fontSize: Float,
    lineHeight: Float,
    theme: ReaderTheme,
    startPage: Int,
    onPageChanged: (Int, Int) -> Unit,
    onTap: () -> Unit,
) {
    val bgColor = when (theme) {
        ReaderTheme.DARK  -> "#0F172A"
        ReaderTheme.SEPIA -> "#F4ECD8"
        ReaderTheme.LIGHT -> "#FFFFFF"
    }
    val textColor = when (theme) {
        ReaderTheme.DARK  -> "#E2E8F0"
        ReaderTheme.SEPIA -> "#3B2F1E"
        ReaderTheme.LIGHT -> "#1A1A2E"
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = WebViewClient()
                setBackgroundColor(android.graphics.Color.parseColor(bgColor))
            }
        },
        update = { webView ->
            // Inject CSS for current theme/font settings
            val css = """
                body {
                    background-color: $bgColor !important;
                    color: $textColor !important;
                    font-size: ${fontSize}px !important;
                    line-height: $lineHeight !important;
                    padding: 16px !important;
                    font-family: 'Georgia', serif !important;
                }
            """.trimIndent()
            val js = "document.querySelector('style#reader-css') ? " +
                "document.querySelector('style#reader-css').innerHTML = `$css` : " +
                "document.head.insertAdjacentHTML('beforeend', `<style id=\"reader-css\">$css</style>`)"
            webView.evaluateJavascript(js, null)

            if (webView.url != fileUrl) {
                webView.loadUrl(fileUrl)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

// ─── Top Bar ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkPanel: () -> Unit,
    onFontPanel: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium)
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
        },
        actions = {
            IconButton(onClick = onBookmark) { Icon(Icons.Outlined.BookmarkAdd, "Add bookmark") }
            IconButton(onClick = onBookmarkPanel) { Icon(Icons.Outlined.Bookmarks, "Bookmarks") }
            IconButton(onClick = onFontPanel) { Icon(Icons.Outlined.TextFormat, "Font") }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
    )
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────
@Composable
private fun ReaderBottomBar(
    current: Int,
    total: Int,
    onPageJump: (Int) -> Unit,
    onThemeLight: () -> Unit,
    onThemeDark: () -> Unit,
    onThemeSepia: () -> Unit,
    currentTheme: ReaderTheme,
) {
    var showPageInput by remember { mutableStateOf(false) }
    var pageInputText by remember { mutableStateOf("") }
    val progress = if (total > 0) current.toFloat() / total else 0f

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Progress Slider
            Slider(
                value = progress,
                onValueChange = { onPageJump((it * total).toInt().coerceAtLeast(1)) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = Emerald600, activeTrackColor = Emerald600),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Page info
                TextButton(onClick = { showPageInput = true }) {
                    Text(
                        text = "Page $current / $total",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                // Progress %
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Theme buttons
                Row {
                    ThemeButton("A", Color.White, Color.Black, currentTheme == ReaderTheme.LIGHT, onThemeLight)
                    ThemeButton("A", SepiaBackground, SepiaText, currentTheme == ReaderTheme.SEPIA, onThemeSepia)
                    ThemeButton("A", BackgroundDark, Color.White, currentTheme == ReaderTheme.DARK, onThemeDark)
                }
            }
        }
    }

    // Page Jump Dialog
    if (showPageInput) {
        AlertDialog(
            onDismissRequest = { showPageInput = false },
            title = { Text("Go to Page") },
            text = {
                OutlinedTextField(
                    value = pageInputText,
                    onValueChange = { pageInputText = it.filter { c -> c.isDigit() } },
                    label = { Text("Page number (1 - $total)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pageInputText.toIntOrNull()?.let { page ->
                        onPageJump(page.coerceIn(1, total))
                    }
                    showPageInput = false
                    pageInputText = ""
                }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showPageInput = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThemeButton(
    text: String, bg: Color, textColor: Color,
    selected: Boolean, onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) Emerald600 else Color.LightGray,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.labelSmall)
    }
}

// ─── Bookmark Panel ───────────────────────────────────────────────────────
@Composable
private fun BookmarkPanel(
    bookmarks: List<Bookmark>,
    onGoToBookmark: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onClose: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).clickable(onClick = onClose))
        // Panel
        Surface(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.75f).align(Alignment.CenterEnd),
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            shadowElevation = 16.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bookmarks", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
                }
                HorizontalDivider()
                if (bookmarks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No bookmarks yet", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(8.dp)) {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            BookmarkItem(
                                bookmark = bookmark,
                                onClick = { onGoToBookmark(bookmark); onClose() },
                                onDelete = { onDeleteBookmark(bookmark) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkItem(bookmark: Bookmark, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Bookmark, null, tint = Emerald600, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bookmark.label, style = MaterialTheme.typography.labelMedium)
                Text("Page ${bookmark.page + 1}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (bookmark.note.isNotBlank()) {
                    Text(bookmark.note, style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ─── Font Panel ───────────────────────────────────────────────────────────
@Composable
private fun FontPanel(
    fontSize: Float,
    lineHeight: Float,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onClose: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).clickable(onClick = onClose))
        Surface(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Reading Settings", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
                }
                Spacer(Modifier.height(16.dp))

                // Font Size
                Text("Font Size: ${fontSize.toInt()}pt", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onFontSizeChange(fontSize - 1) }) {
                        Icon(Icons.Filled.Remove, "Decrease")
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = onFontSizeChange,
                        valueRange = 10f..30f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Emerald600, activeTrackColor = Emerald600),
                    )
                    IconButton(onClick = { onFontSizeChange(fontSize + 1) }) {
                        Icon(Icons.Filled.Add, "Increase")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Line Height
                Text("Line Spacing: ${"%.1f".format(lineHeight)}x", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onLineHeightChange(lineHeight - 0.1f) }) {
                        Icon(Icons.Filled.FormatLineSpacing, "Decrease")
                    }
                    Slider(
                        value = lineHeight,
                        onValueChange = onLineHeightChange,
                        valueRange = 1.0f..2.5f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Emerald600, activeTrackColor = Emerald600),
                    )
                    IconButton(onClick = { onLineHeightChange(lineHeight + 0.1f) }) {
                        Icon(Icons.Filled.FormatLineSpacing, "Increase")
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReaderLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Emerald600)
            Spacer(Modifier.height(16.dp))
            Text("Opening book...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReaderError(error: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Filled.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = ErrorRed)
            Spacer(Modifier.height(16.dp))
            Text("Failed to open book", style = MaterialTheme.typography.titleMedium)
            Text(error, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) { Text("Go Back") }
        }
    }
}
