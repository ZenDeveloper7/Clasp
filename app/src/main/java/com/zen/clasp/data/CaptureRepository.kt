package com.zen.clasp.data

import android.content.ContentResolver
import android.net.Uri
import com.zen.clasp.model.Attachment
import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.DeletionState
import com.zen.clasp.model.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

data class ExportSpec(
    val mimeType: String,
    val suggestedFileName: String
)

interface CaptureRepository {
    val captures: Flow<List<Capture>>

    suspend fun createText(text: String, sourcePackage: String? = null): String
    suspend fun importUri(uri: Uri, declaredMimeType: String?, sourcePackage: String? = null): String
    suspend fun update(captureId: String, title: String?, note: String?)
    suspend fun setFavorite(captureId: String, favorite: Boolean)
    suspend fun delete(captureId: String)
    suspend fun exportSpec(captureId: String): ExportSpec
    suspend fun export(captureId: String, destination: Uri)
}

class DefaultCaptureRepository(
    private val captureDao: CaptureDao,
    private val attachmentStore: FileAttachmentStore,
    private val contentResolver: ContentResolver,
    private val now: () -> Long = System::currentTimeMillis
) : CaptureRepository {
    override val captures: Flow<List<Capture>> = captureDao.observeAll().map { captures ->
        captures.map { capture -> capture.toModel() }
    }

    override suspend fun createText(text: String, sourcePackage: String?): String {
        val content = text.trim()
        require(content.isNotEmpty()) { "Text cannot be empty" }
        val id = UUID.randomUUID().toString()
        val timestamp = now()
        captureDao.insert(
            capture = CaptureEntity(
                id = id,
                type = CaptureType.TEXT.name,
                createdAt = timestamp,
                updatedAt = timestamp,
                sourcePackage = sourcePackage,
                originalText = content,
                userTitle = null,
                userNote = null,
                isFavorite = false,
                processingState = ProcessingState.STORED.name,
                deletionState = DeletionState.ACTIVE.name,
                errorCode = null
            ),
            attachment = null
        )
        return id
    }

    override suspend fun importUri(
        uri: Uri,
        declaredMimeType: String?,
        sourcePackage: String?
    ): String {
        val id = UUID.randomUUID().toString()
        val stored = attachmentStore.copyFrom(contentResolver, uri, id, declaredMimeType)
        val timestamp = now()
        try {
            captureDao.insert(
                capture = CaptureEntity(
                    id = id,
                    type = if (stored.mimeType.startsWith("image/")) {
                        CaptureType.IMAGE.name
                    } else {
                        CaptureType.FILE.name
                    },
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    sourcePackage = sourcePackage,
                    originalText = null,
                    userTitle = null,
                    userNote = null,
                    isFavorite = false,
                    processingState = ProcessingState.STORED.name,
                    deletionState = DeletionState.ACTIVE.name,
                    errorCode = null
                ),
                attachment = AttachmentEntity(
                    id = stored.id,
                    captureId = id,
                    relativePath = stored.relativePath,
                    originalDisplayName = stored.originalDisplayName,
                    mimeType = stored.mimeType,
                    sizeBytes = stored.sizeBytes,
                    sha256 = stored.sha256
                )
            )
        } catch (error: Throwable) {
            runCatching { attachmentStore.deleteCapture(id) }
            throw error
        }
        return id
    }

    override suspend fun update(captureId: String, title: String?, note: String?) {
        captureDao.updateEditableFields(
            captureId = captureId,
            title = title?.trim()?.takeIf(String::isNotEmpty),
            note = note?.trim()?.takeIf(String::isNotEmpty),
            updatedAt = now()
        )
    }

    override suspend fun setFavorite(captureId: String, favorite: Boolean) {
        captureDao.updateFavorite(captureId, favorite, now())
    }

    override suspend fun delete(captureId: String) {
        captureDao.updateDeletionState(
            captureId,
            DeletionState.DELETING.name,
            errorCode = null,
            updatedAt = now()
        )
        try {
            attachmentStore.deleteCapture(captureId)
            captureDao.deleteById(captureId)
        } catch (error: Throwable) {
            captureDao.updateDeletionState(
                captureId,
                DeletionState.FAILED.name,
                errorCode = DELETE_FAILED,
                updatedAt = now()
            )
            throw error
        }
    }

    override suspend fun exportSpec(captureId: String): ExportSpec {
        val capture = captureDao.getById(captureId)?.toModel()
            ?: throw IllegalArgumentException("Capture does not exist")
        val attachment = capture.attachments.firstOrNull()
        return if (attachment != null) {
            ExportSpec(
                mimeType = attachment.mimeType,
                suggestedFileName = attachmentStore.suggestedFileName(
                    attachment.originalDisplayName,
                    attachment.mimeType
                )
            )
        } else {
            ExportSpec("text/plain", "clasp-${capture.createdAt}.txt")
        }
    }

    override suspend fun export(captureId: String, destination: Uri) {
        val capture = captureDao.getById(captureId)?.toModel()
            ?: throw IllegalArgumentException("Capture does not exist")
        val attachment = capture.attachments.firstOrNull()
        if (attachment != null) {
            attachmentStore.exportTo(contentResolver, attachment.relativePath, destination)
            return
        }

        withContext(Dispatchers.IO) {
            val content = buildString {
                append(capture.userTitle?.let { "$it\n\n" }.orEmpty())
                append(capture.originalText.orEmpty())
                append(capture.userNote?.let { "\n\nNote\n$it" }.orEmpty())
            }
            contentResolver.openOutputStream(destination, "wt")?.bufferedWriter()?.use { writer ->
                writer.write(content)
            } ?: throw IOException("Unable to open export destination")
        }
    }

    suspend fun resumeInterruptedDeletions() {
        captureDao.getInterruptedDeletions().forEach { capture ->
            runCatching { delete(capture.capture.id) }
        }
    }

    private fun CaptureWithAttachments.toModel() = Capture(
        id = capture.id,
        type = CaptureType.fromStorage(capture.type),
        createdAt = capture.createdAt,
        updatedAt = capture.updatedAt,
        sourcePackage = capture.sourcePackage,
        originalText = capture.originalText,
        userTitle = capture.userTitle,
        userNote = capture.userNote,
        isFavorite = capture.isFavorite,
        processingState = ProcessingState.fromStorage(capture.processingState),
        deletionState = DeletionState.fromStorage(capture.deletionState),
        errorCode = capture.errorCode,
        attachments = attachments.map { attachment ->
            Attachment(
                id = attachment.id,
                relativePath = attachment.relativePath,
                originalDisplayName = attachment.originalDisplayName,
                mimeType = attachment.mimeType,
                sizeBytes = attachment.sizeBytes,
                sha256 = attachment.sha256
            )
        }
    )

    companion object {
        private const val DELETE_FAILED = "DELETE_FAILED"
    }
}
