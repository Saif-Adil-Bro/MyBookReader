package com.myreader.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.app.domain.model.BookRequest
import com.myreader.app.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.myreader.app.presentation.theme.*

// ─── Book Request ViewModel ───────────────────────────────────────────────
@HiltViewModel
class BookRequestViewModel @Inject constructor(
    private val bookRepo: BookRepository,
) : ViewModel() {

    private val _submitted = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    fun submitRequest(title: String, author: String, description: String) {
        if (title.isBlank()) {
            _error.value = "Book title is required"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            bookRepo.submitBookRequest(
                BookRequest(bookTitle = title.trim(), authorName = author.trim(), description = description.trim())
            ).onSuccess {
                _submitted.value = true
            }.onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRequestScreen(
    onBack: () -> Unit,
    viewModel: BookRequestViewModel = hiltViewModel(),
) {
    val submitted by viewModel.submitted.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var bookTitle by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request a Book") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            )
        }
    ) { padding ->
        if (submitted) {
            // Success state
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text("✅", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("Request Submitted!", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Thank you! Our team will review your request and add the book soon.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = onBack, shape = RoundedCornerShape(12.dp)) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    Icons.Filled.LibraryAdd, null,
                    modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
                    tint = Emerald600,
                )
                Text(
                    "Can't find a book you're looking for? Let us know and we'll try to add it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                HorizontalDivider()

                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text("Book Title *") },
                    leadingIcon = { Icon(Icons.Filled.Book, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("Author Name (optional)") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Additional Info (optional)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("ISBN, edition, language, etc.") },
                    maxLines = 5,
                )

                error?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = { viewModel.submitRequest(bookTitle, authorName, description) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = White, strokeWidth = 2.dp)
                    else Text("Submit Request")
                }
            }
        }
    }
}

// ─── Report Broken Book ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBrokenBookDialog(
    bookId: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val reasons = listOf(
        "File won't open",
        "File is corrupt or incomplete",
        "Wrong file (different book)",
        "Download link is broken",
        "Missing pages / content",
        "Other",
    )
    var selectedReason by remember { mutableStateOf("") }
    var otherText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Flag, null, tint = WarningAmber) },
        title = { Text("Report an Issue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("What's wrong with this book?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = Emerald600),
                        )
                        Text(reason, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f))
                    }
                }
                if (selectedReason == "Other") {
                    OutlinedTextField(
                        value = otherText,
                        onValueChange = { otherText = it },
                        label = { Text("Describe the issue") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reason = if (selectedReason == "Other") otherText else selectedReason
                    onSubmit(reason)
                    onDismiss()
                },
                enabled = selectedReason.isNotBlank() && (selectedReason != "Other" || otherText.isNotBlank()),
            ) { Text("Report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
