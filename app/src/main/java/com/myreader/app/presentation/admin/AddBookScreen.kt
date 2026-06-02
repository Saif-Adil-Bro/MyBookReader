package com.myreader.app.presentation.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
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
data class AddBookState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddBookState())
    val state: StateFlow<AddBookState> = _state.asStateFlow()

    fun submitBook(
        title: String, author: String, description: String,
        category: BookCategory, language: BookLanguage,
        format: BookFormat, isFeatured: Boolean,
        publishedYear: String, isbn: String, publisher: String,
        coverUri: Uri, fileUri: Uri,
    ) {
        if (title.isBlank() || author.isBlank() || coverUri == Uri.EMPTY || fileUri == Uri.EMPTY) {
            _state.update { it.copy(error = "Please fill in all required fields and select files.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val book = Book(
                title = title.trim(),
                author = author.trim(),
                description = description.trim(),
                category = category,
                language = language,
                fileFormat = format,
                isFeatured = isFeatured,
                isActive = true,
                publishedYear = publishedYear.toIntOrNull() ?: 0,
                isbn = isbn.trim(),
                publisher = publisher.trim(),
            )
            adminRepo.addBook(book, coverUri, fileUri)
                .onSuccess { _state.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

// ─── Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AddBookViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Form fields
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(BookCategory.NOVELS) }
    var language by remember { mutableStateOf(BookLanguage.ENGLISH) }
    var format by remember { mutableStateOf(BookFormat.PDF) }
    var isFeatured by remember { mutableStateOf(false) }
    var publishedYear by remember { mutableStateOf("") }
    var isbn by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf(Uri.EMPTY) }
    var fileUri by remember { mutableStateOf(Uri.EMPTY) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    // File pickers
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { coverUri = it }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { fileUri = it }
    }

    LaunchedEffect(state.success) { if (state.success) onSuccess() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Book") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.submitBook(
                                title, author, description, category, language,
                                format, isFeatured, publishedYear, isbn, publisher,
                                coverUri, fileUri,
                            )
                        },
                        enabled = !state.isLoading,
                    ) {
                        if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Publish", color = Emerald600)
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── Cover image picker ─────────────────────────────────────────
            Text("Book Cover *", style = MaterialTheme.typography.labelLarge)
            Box(
                modifier = Modifier
                    .size(120.dp, 170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        2.dp,
                        if (coverUri == Uri.EMPTY) MaterialTheme.colorScheme.outline else Emerald600,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { coverPicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (coverUri != Uri.EMPTY) {
                    AsyncImage(model = coverUri, contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AddPhotoAlternate, null,
                            modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Add Cover", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Book file picker ──────────────────────────────────────────
            Text("Book File (PDF/EPUB) *", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { filePicker.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    if (fileUri == Uri.EMPTY) Icons.Outlined.UploadFile else Icons.Filled.CheckCircle,
                    null, tint = if (fileUri == Uri.EMPTY) LocalContentColor.current else Emerald600,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (fileUri == Uri.EMPTY) "Select PDF or EPUB File" else "File Selected ✓")
            }

            HorizontalDivider()
            Text("Book Information", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            // ── Text fields ───────────────────────────────────────────────
            FormField("Title *", title, { title = it })
            FormField("Author *", author, { author = it })
            FormField(
                "Description", description, { description = it },
                singleLine = false, minLines = 3, maxLines = 6,
            )
            FormField("Published Year", publishedYear, { publishedYear = it },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            FormField("ISBN", isbn, { isbn = it })
            FormField("Publisher", publisher, { publisher = it })

            // ── Dropdowns ─────────────────────────────────────────────────
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = category.displayNameEn,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    BookCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayNameEn) },
                            onClick = { category = cat; categoryExpanded = false },
                            leadingIcon = if (cat == category) {{ Icon(Icons.Filled.Check, null, tint = Emerald600) }} else null,
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = langExpanded, onExpandedChange = { langExpanded = it }) {
                OutlinedTextField(
                    value = language.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                    BookLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.displayName) },
                            onClick = { language = lang; langExpanded = false },
                        )
                    }
                }
            }

            // ── Format selector ───────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = format == fmt,
                        onClick = { format = fmt },
                        label = { Text(fmt.name) },
                        leadingIcon = if (format == fmt) {{ Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }} else null,
                    )
                }
            }

            // ── Featured toggle ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Featured Book", style = MaterialTheme.typography.bodyLarge)
                    Text("Show in home banner slider", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isFeatured, onCheckedChange = { isFeatured = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = Emerald600))
            }

            // ── Error message ─────────────────────────────────────────────
            state.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Publish button ────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.submitBook(
                        title, author, description, category, language,
                        format, isFeatured, publishedYear, isbn, publisher,
                        coverUri, fileUri,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Publishing...")
                } else {
                    Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Publish Book", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
    )
}
