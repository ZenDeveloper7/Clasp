package com.zen.clasp

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zen.clasp.data.AttachmentTooLargeException
import com.zen.clasp.data.CaptureRepository
import com.zen.clasp.data.ExportSpec
import com.zen.clasp.model.Capture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExportRequest(
    val captureId: String,
    val spec: ExportSpec
)

class MainViewModel(private val repository: CaptureRepository) : ViewModel() {
    val captures: StateFlow<List<Capture>> = repository.captures.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _exportRequest = MutableStateFlow<ExportRequest?>(null)
    val exportRequest = _exportRequest.asStateFlow()

    fun createText(text: String, onSaved: () -> Unit) {
        launchOperation(successMessage = "Text saved", onSuccess = onSaved) {
            repository.createText(text)
        }
    }

    fun importUri(uri: Uri, mimeType: String? = null, sourcePackage: String? = null) {
        launchOperation(successMessage = "Capture saved") {
            repository.importUri(uri, mimeType, sourcePackage)
        }
    }

    fun handleIncomingIntent(intent: Intent, sourcePackage: String? = null) {
        if (intent.action != Intent.ACTION_SEND) return
        val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

        when {
            stream != null -> importUri(stream, intent.type, sourcePackage)
            !sharedText.isNullOrBlank() -> launchOperation(successMessage = "Shared text saved") {
                repository.createText(sharedText, sourcePackage)
            }
            else -> _message.value = "That shared item did not contain supported content"
        }
    }

    fun update(captureId: String, title: String, note: String, onSaved: () -> Unit) {
        launchOperation(successMessage = "Changes saved", onSuccess = onSaved) {
            repository.update(captureId, title, note)
        }
    }

    fun setFavorite(capture: Capture) {
        launchOperation {
            repository.setFavorite(capture.id, !capture.isFavorite)
        }
    }

    fun delete(captureId: String, onDeleted: () -> Unit) {
        launchOperation(successMessage = "Capture permanently deleted", onSuccess = onDeleted) {
            repository.delete(captureId)
        }
    }

    fun requestExport(captureId: String) {
        launchOperation {
            _exportRequest.value = ExportRequest(captureId, repository.exportSpec(captureId))
        }
    }

    fun exportTo(captureId: String, destination: Uri) {
        launchOperation(successMessage = "Capture exported") {
            repository.export(captureId, destination)
        }
    }

    fun consumeExportRequest() {
        _exportRequest.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun launchOperation(
        successMessage: String? = null,
        onSuccess: () -> Unit = {},
        operation: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                operation()
                successMessage?.let { _message.value = it }
                onSuccess()
            } catch (_: AttachmentTooLargeException) {
                _message.value = "Files must be 100 MB or smaller"
            } catch (_: SecurityException) {
                _message.value = "Clasp could not read that item"
            } catch (error: IllegalArgumentException) {
                _message.value = error.message ?: "That item is not supported"
            } catch (_: Exception) {
                _message.value = "The operation failed. Your existing captures are unchanged."
            } finally {
                _isBusy.value = false
            }
        }
    }

    companion object {
        fun factory(repository: CaptureRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(MainViewModel::class.java))
                    return MainViewModel(repository) as T
                }
            }
    }
}
