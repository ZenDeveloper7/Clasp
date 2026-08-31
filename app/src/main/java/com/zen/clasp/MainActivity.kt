package com.zen.clasp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.DeletionState
import com.zen.clasp.model.ExtractionState
import com.zen.clasp.model.ProcessingState
import com.zen.clasp.search.CandidateExtractor
import com.zen.clasp.search.ExtractionCandidate
import com.zen.clasp.search.ExtractionFilter
import com.zen.clasp.search.SearchDateRange
import com.zen.clasp.search.SearchFilters
import com.zen.clasp.search.SearchResult
import com.zen.clasp.ui.theme.ClaspTheme
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel> {
        MainViewModel.factory((application as ClaspApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) viewModel.handleIncomingIntent(intent, callingPackage)
        setContent {
            ClaspTheme {
                ClaspApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIncomingIntent(intent, callingPackage)
    }
}

private enum class Screen {
    LIBRARY,
    CREATE_TEXT,
    SEARCH,
    DETAIL
}

@Composable
private fun ClaspApp(viewModel: MainViewModel) {
    val captures by viewModel.captures.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val exportRequest by viewModel.exportRequest.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchFilters by viewModel.searchFilters.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var screenName by rememberSaveable { mutableStateOf(Screen.LIBRARY.name) }
    var selectedCaptureId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailOriginName by rememberSaveable { mutableStateOf(Screen.LIBRARY.name) }
    val screen = Screen.entries.firstOrNull { it.name == screenName } ?: Screen.LIBRARY

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.importUri(it) }
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importUri(it) }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val request = exportRequest
        val destination = result.data?.data
        viewModel.consumeExportRequest()
        if (request != null && destination != null) {
            viewModel.exportTo(request.captureId, destination)
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(exportRequest) {
        exportRequest?.let { request ->
            exportLauncher.launch(
                Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(request.spec.mimeType)
                    .putExtra(Intent.EXTRA_TITLE, request.spec.suggestedFileName)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.LIBRARY -> LibraryScreen(
                captures = captures,
                snackbarHostState = snackbarHostState,
                onCreateText = { screenName = Screen.CREATE_TEXT.name },
                onPickImage = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickFile = { documentPicker.launch(arrayOf("*/*")) },
                onSearch = { screenName = Screen.SEARCH.name },
                onCaptureSelected = {
                    selectedCaptureId = it
                    detailOriginName = Screen.LIBRARY.name
                    screenName = Screen.DETAIL.name
                }
            )

            Screen.CREATE_TEXT -> TextCaptureScreen(
                snackbarHostState = snackbarHostState,
                onBack = { screenName = Screen.LIBRARY.name },
                onSave = { text ->
                    viewModel.createText(text) { screenName = Screen.LIBRARY.name }
                }
            )

            Screen.SEARCH -> SearchScreen(
                query = searchQuery,
                filters = searchFilters,
                results = searchResults,
                snackbarHostState = snackbarHostState,
                onQueryChanged = viewModel::updateSearchQuery,
                onFiltersChanged = viewModel::updateSearchFilters,
                onBack = { screenName = Screen.LIBRARY.name },
                onCaptureSelected = {
                    selectedCaptureId = it
                    detailOriginName = Screen.SEARCH.name
                    screenName = Screen.DETAIL.name
                }
            )

            Screen.DETAIL -> DetailScreen(
                capture = captures.firstOrNull { it.id == selectedCaptureId },
                snackbarHostState = snackbarHostState,
                onBack = { screenName = detailOriginName },
                onSave = { captureId, title, note ->
                    viewModel.update(captureId, title, note) {}
                },
                onFavorite = viewModel::setFavorite,
                onExport = viewModel::requestExport,
                onRetryOcr = viewModel::retryOcr,
                onDelete = { captureId ->
                    viewModel.delete(captureId) {
                        selectedCaptureId = null
                        screenName = detailOriginName
                    }
                }
            )
        }

        if (isBusy) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(
    captures: List<Capture>,
    snackbarHostState: SnackbarHostState,
    onCreateText: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onSearch: () -> Unit,
    onCaptureSelected: (String) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CLASP", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "CAPTURE / LIBRARY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.action_search)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("CAPTURE", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onCreateText, modifier = Modifier.weight(1f)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_note_add),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Text")
                    }
                    OutlinedButton(onClick = onPickImage, modifier = Modifier.weight(1f)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_image),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Image")
                    }
                    OutlinedButton(onClick = onPickFile, modifier = Modifier.weight(1f)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_attach_file),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("File")
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("LIBRARY / ${captures.size}", style = MaterialTheme.typography.labelMedium)
            }

            if (captures.isEmpty()) {
                item { EmptyLibrary() }
            } else {
                items(captures, key = Capture::id) { capture ->
                    CaptureRow(capture, onCaptureSelected)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    query: String,
    filters: SearchFilters,
    results: List<SearchResult>,
    snackbarHostState: SnackbarHostState,
    onQueryChanged: (String) -> Unit,
    onFiltersChanged: (SearchFilters) -> Unit,
    onBack: () -> Unit,
    onCaptureSelected: (String) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("SEARCH") },
                navigationIcon = { BackIconButton(onBack) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search saved text, notes, files and OCR") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null
                        )
                    },
                    singleLine = true
                )
            }
            item {
                Text("FILTERS", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val nextType = when (filters.type) {
                                null -> CaptureType.TEXT
                                CaptureType.TEXT -> CaptureType.IMAGE
                                CaptureType.IMAGE -> CaptureType.FILE
                                CaptureType.FILE -> null
                            }
                            onFiltersChanged(filters.copy(type = nextType))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(filters.type?.displayName ?: "All types", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            onFiltersChanged(filters.copy(favouriteOnly = !filters.favouriteOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (filters.favouriteOnly) "Favourites" else "Any saved", maxLines = 1)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val next = SearchDateRange.entries[
                                (filters.dateRange.ordinal + 1) % SearchDateRange.entries.size
                            ]
                            onFiltersChanged(filters.copy(dateRange = next))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(filters.dateRange.displayName, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            val next = ExtractionFilter.entries[
                                (filters.extraction.ordinal + 1) % ExtractionFilter.entries.size
                            ]
                            onFiltersChanged(filters.copy(extraction = next))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(filters.extraction.displayName, maxLines = 1)
                    }
                }
            }
            when {
                query.isBlank() -> item {
                    Text(
                        "Search is local and works offline. OCR text is labelled separately from the original.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                results.isEmpty() -> item { Text("No matching captures") }
                else -> items(results, key = { it.capture.id }) { result ->
                    SearchResultRow(result, onCaptureSelected)
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, onCaptureSelected: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCaptureSelected(result.capture.id) },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(result.matchedField.displayName.uppercase(), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(result.capture.displayTitle, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(result.excerpt, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("Nothing clasped yet", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Create a note, choose an image or file, or share something to Clasp from another app.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CaptureRow(capture: Capture, onCaptureSelected: (String) -> Unit) {
    val date = remember(capture.createdAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(capture.createdAt))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCaptureSelected(capture.id) },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(capture.type.displayName.uppercase(), style = MaterialTheme.typography.labelMedium)
                Text(if (capture.isFavorite) "FAVOURITE" else date, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                capture.displayTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            capture.previewText?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (capture.deletionState == DeletionState.FAILED) {
                Spacer(Modifier.height(8.dp))
                Text("DELETE NEEDS RETRY", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextCaptureScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("NEW TEXT") },
                navigationIcon = { BackIconButton(onBack) },
                actions = {
                    IconButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save),
                            contentDescription = stringResource(R.string.action_save)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            label = { Text("What do you want to remember?") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    capture: Capture?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onFavorite: (Capture) -> Unit,
    onExport: (String) -> Unit,
    onRetryOcr: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by rememberSaveable(capture?.id) { mutableStateOf(capture?.userTitle.orEmpty()) }
    var note by rememberSaveable(capture?.id) { mutableStateOf(capture?.userNote.orEmpty()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val candidates by rememberExtractionCandidates(capture)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("CAPTURE DETAIL") },
                navigationIcon = { BackIconButton(onBack) }
            )
        }
    ) { innerPadding ->
        if (capture == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Capture is unavailable")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "${capture.type.displayName.uppercase()} / ${capture.processingState.name}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            capture.originalText?.let { original ->
                item { ProvenanceSection("ORIGINAL", original) }
            }
            capture.attachments.firstOrNull()?.let { attachment ->
                item {
                    ProvenanceSection(
                        "ORIGINAL FILE",
                        buildString {
                            append(attachment.originalDisplayName ?: "Imported item")
                            append("\n${attachment.mimeType}")
                            append(" · ${formatBytes(attachment.sizeBytes)}")
                        }
                    )
                }
            }
            if (capture.type == CaptureType.IMAGE) {
                item {
                    OcrStatusSection(
                        state = capture.extractionState,
                        errorCode = capture.extractionErrorCode,
                        onRetry = { onRetryOcr(capture.id) }
                    )
                }
            }
            capture.extractedText?.let { extracted ->
                item { ProvenanceSection("EXTRACTED / OCR", extracted) }
            }
            if (candidates.isNotEmpty()) {
                item {
                    ProvenanceSection(
                        "DETECTED CANDIDATES",
                        candidates.joinToString("\n") { candidate ->
                            "${candidate.type.displayName}: ${candidate.value}"
                        }
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    minLines = 3
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSave(capture.id, title, note) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Save changes")
                    }
                    OutlinedButton(
                        onClick = { onFavorite(capture) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (capture.isFavorite) {
                                    R.drawable.ic_favorite_filled
                                } else {
                                    R.drawable.ic_favorite_outline
                                }
                            ),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (capture.isFavorite) "Unfavourite" else "Favourite")
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onExport(capture.id) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_export),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export")
                    }
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Delete now", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDelete && capture != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Permanently delete capture?") },
            text = { Text("The original and Clasp-owned copies will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(capture.id)
                }) {
                    Text("Delete now", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun rememberExtractionCandidates(capture: Capture?) = produceState<List<ExtractionCandidate>>(
    initialValue = emptyList(),
    key1 = capture?.id,
    key2 = capture?.contentRevision
) {
    if (capture == null) return@produceState
    val sourceText = listOfNotNull(capture.originalText, capture.extractedText).joinToString("\n")
    value = withContext(Dispatchers.Default) {
        CandidateExtractor.extract(sourceText)
    }
}

@Composable
private fun OcrStatusSection(
    state: ExtractionState,
    errorCode: String?,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("OCR / ${state.name.replace('_', ' ')}", style = MaterialTheme.typography.labelMedium)
        when (state) {
            ExtractionState.PENDING -> Text("Queued for private on-device text recognition.")
            ExtractionState.RUNNING -> Text("Reading text on this device…")
            ExtractionState.COMPLETE -> Text("Extracted text is stored locally and searchable.")
            ExtractionState.EMPTY -> Text("No readable text was found in this image.")
            ExtractionState.FAILED -> {
                Text(
                    "Text recognition failed${errorCode?.let { ": $it" }.orEmpty()}.",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Retry OCR")
                }
            }
            ExtractionState.NOT_APPLICABLE -> Unit
        }
    }
}

@Composable
private fun ProvenanceSection(label: String, content: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Icon(
            painter = painterResource(R.drawable.ic_clasp_rail),
            contentDescription = null,
            modifier = Modifier
                .width(24.dp)
                .height(48.dp)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Text(content)
        }
    }
}

@Composable
private fun BackIconButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.action_back)
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@Preview(showBackground = true)
@Composable
private fun LibraryPreview() {
    ClaspTheme {
        LibraryScreen(
            captures = listOf(
                Capture(
                    id = "preview",
                    type = CaptureType.TEXT,
                    createdAt = 0,
                    updatedAt = 0,
                    sourcePackage = null,
                    originalText = "A local-first capture that remains available offline.",
                    userTitle = "Clasp foundation",
                    userNote = null,
                    isFavorite = true,
                    processingState = ProcessingState.STORED,
                    extractedText = null,
                    extractionState = ExtractionState.NOT_APPLICABLE,
                    extractionErrorCode = null,
                    contentRevision = 1,
                    deletionState = DeletionState.ACTIVE,
                    errorCode = null,
                    attachments = emptyList()
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onCreateText = {},
            onPickImage = {},
            onPickFile = {},
            onSearch = {},
            onCaptureSelected = {}
        )
    }
}
