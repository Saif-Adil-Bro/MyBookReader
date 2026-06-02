package com.myreader.app.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.domain.model.*
import com.myreader.app.domain.repository.*
import com.myreader.app.presentation.library.BookListItem
import com.myreader.app.presentation.library.EmptyState
import com.myreader.app.presentation.theme.Emerald600
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ─── Search ViewModel ─────────────────────────────────────────────────────
@HiltViewModel
class SearchViewModel @Inject constructor(private val bookRepo: BookRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<Book>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else bookRepo.searchBooks(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }
    fun clearQuery() { _query.value = "" }
}

// ─── Search Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search books, authors...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = viewModel::clearQuery) { Icon(Icons.Filled.Clear, "Clear") }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald600,
                        )
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            )
        }
    ) { padding ->
        when {
            query.isBlank() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                        Spacer(Modifier.height(16.dp))
                        Text("Search for books", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            results.isEmpty() -> {
                EmptyState(Icons.Outlined.SearchOff, "No results for \"$query\"",
                    "Try different keywords or check spelling")
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = 16.dp,
                        start = 16.dp, end = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text("${results.size} result${if (results.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(results, key = { it.id }) { book ->
                        BookListItem(book = book, onClick = { onBookClick(book.id) })
                    }
                }
            }
        }
    }
}

// ─── Category ViewModel & Screen ──────────────────────────────────────────
@HiltViewModel
class CategoryViewModel @Inject constructor(private val bookRepo: BookRepository) : ViewModel() {
    private val _category = MutableStateFlow<BookCategory?>(null)

    val books: StateFlow<List<Book>> = _category
        .filterNotNull()
        .flatMapLatest { bookRepo.getBooksByCategory(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(name: String) { _category.value = BookCategory.fromString(name) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    categoryName: String,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsState()
    val category = remember(categoryName) { BookCategory.fromString(categoryName) }

    LaunchedEffect(categoryName) { viewModel.setCategory(categoryName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.displayNameEn) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            )
        }
    ) { padding ->
        if (books.isEmpty()) {
            EmptyState(Icons.Outlined.LibraryBooks, "No books in this category",
                "Check back later for new additions")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                    start = 16.dp, end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text("${books.size} books", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                }
                items(books, key = { it.id }) { book ->
                    BookListItem(book = book, onClick = { onBookClick(book.id) })
                }
            }
        }
    }
}
