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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.DeletionState
import com.zen.clasp.model.ProcessingState
import com.zen.clasp.ui.theme.ClaspTheme
import java.text.DateFormat
import java.util.Date

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
    DETAIL
}

@Composable
private fun ClaspApp(viewModel: MainViewModel) {
    val captures by viewModel.captures.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val exportRequest by viewModel.exportRequest.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var screenName by rememberSaveable { mutableStateOf(Screen.LIBRARY.name) }
    var selectedCaptureId by rememberSaveable { mutableStateOf<String?>(null) }
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
                onCaptureSelected = {
                    selectedCaptureId = it
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

            Screen.DETAIL -> DetailScreen(
                capture = captures.firstOrNull { it.id == selectedCaptureId },
                snackbarHostState = snackbarHostState,
                onBack = { screenName = Screen.LIBRARY.name },
                onSave = { captureId, title, note ->
                    viewModel.update(captureId, title, note) {}
                },
                onFavorite = viewModel::setFavorite,
                onExport = viewModel::requestExport,
                onDelete = { captureId ->
                    viewModel.delete(captureId) {
                        selectedCaptureId = null
                        screenName = Screen.LIBRARY.name
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
                        Text("Text")
                    }
                    OutlinedButton(onClick = onPickImage, modifier = Modifier.weight(1f)) {
                        Text("Image")
                    }
                    OutlinedButton(onClick = onPickFile, modifier = Modifier.weight(1f)) {
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
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                        Text("Save")
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
    onDelete: (String) -> Unit
) {
    var title by rememberSaveable(capture?.id) { mutableStateOf(capture?.userTitle.orEmpty()) }
    var note by rememberSaveable(capture?.id) { mutableStateOf(capture?.userNote.orEmpty()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("CAPTURE DETAIL") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSave(capture.id, title, note) }) {
                        Text("Save changes")
                    }
                    OutlinedButton(onClick = { onFavorite(capture) }) {
                        Text(if (capture.isFavorite) "Unfavourite" else "Favourite")
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onExport(capture.id) }) {
                        Text("Export")
                    }
                    TextButton(onClick = { confirmDelete = true }) {
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
private fun ProvenanceSection(label: String, content: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("[", style = MaterialTheme.typography.displaySmall)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Text(content)
        }
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
                    deletionState = DeletionState.ACTIVE,
                    errorCode = null,
                    attachments = emptyList()
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onCreateText = {},
            onPickImage = {},
            onPickFile = {},
            onCaptureSelected = {}
        )
    }
}
