package com.zen.clasp.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

data class StoredAttachment(
    val id: String,
    val relativePath: String,
    val originalDisplayName: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String
)

class FileAttachmentStore(private val context: Context) {
    private val capturesRoot = File(context.filesDir, "captures")

    suspend fun copyFrom(
        resolver: ContentResolver,
        uri: Uri,
        captureId: String,
        declaredMimeType: String?
    ): StoredAttachment = withContext(Dispatchers.IO) {
        require(uri.scheme == ContentResolver.SCHEME_CONTENT) {
            "Only content URIs can be imported"
        }

        val metadata = resolver.queryMetadata(uri)
        val mimeType = resolver.getType(uri)
            ?.takeIf(String::isNotBlank)
            ?: declaredMimeType?.takeIf(String::isNotBlank)
            ?: DEFAULT_MIME_TYPE
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.matches(SAFE_EXTENSION) }
            ?: DEFAULT_EXTENSION
        val directory = captureDirectory(captureId).apply {
            check(mkdirs() || isDirectory) { "Unable to create capture storage" }
        }
        val pending = File(directory, ".pending-${UUID.randomUUID()}")
        val destination = File(directory, "payload.$extension")
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L

        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(pending).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        copied += read
                        if (copied > MAX_ATTACHMENT_BYTES) {
                            throw AttachmentTooLargeException(MAX_ATTACHMENT_BYTES)
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            } ?: throw IOException("Unable to open shared content")

            check(pending.renameTo(destination)) { "Unable to commit attachment" }
            StoredAttachment(
                id = UUID.randomUUID().toString(),
                relativePath = destination.relativeTo(context.filesDir).path,
                originalDisplayName = metadata.displayName,
                mimeType = mimeType,
                sizeBytes = copied,
                sha256 = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            )
        } catch (error: Throwable) {
            pending.delete()
            directory.deleteRecursively()
            throw error
        }
    }

    suspend fun exportTo(
        resolver: ContentResolver,
        relativePath: String,
        destination: Uri
    ) = withContext(Dispatchers.IO) {
        val source = resolve(relativePath)
        resolver.openOutputStream(destination, "wt")?.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        } ?: throw IOException("Unable to open export destination")
    }

    suspend fun deleteCapture(captureId: String) = withContext(Dispatchers.IO) {
        val directory = captureDirectory(captureId)
        if (directory.exists() && !directory.deleteRecursively()) {
            throw IOException("Unable to delete capture files")
        }
    }

    fun fileFor(relativePath: String): File = resolve(relativePath)

    fun suggestedFileName(displayName: String?, mimeType: String): String {
        val safeName = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace(UNSAFE_DISPLAY_NAME, "_")
            ?.trim('.', ' ')
            ?.take(80)
            ?.takeIf(String::isNotBlank)
        if (safeName != null) return safeName

        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?.takeIf { it.matches(SAFE_EXTENSION) }
            ?: DEFAULT_EXTENSION
        return "clasp-export.$extension"
    }

    private fun captureDirectory(captureId: String): File {
        require(captureId.matches(UUID_PATTERN)) { "Invalid capture ID" }
        val root = capturesRoot.canonicalFile
        val directory = File(root, captureId).canonicalFile
        require(directory.parentFile == root) { "Invalid capture path" }
        return directory
    }

    private fun resolve(relativePath: String): File {
        val root = capturesRoot.canonicalFile
        val file = File(context.filesDir, relativePath).canonicalFile
        require(file.path.startsWith(root.path + File.separator)) { "Invalid attachment path" }
        require(file.isFile) { "Attachment is unavailable" }
        return file
    }

    private data class ProviderMetadata(val displayName: String?)

    private fun ContentResolver.queryMetadata(uri: Uri): ProviderMetadata = runCatching {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use ProviderMetadata(null)
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            ProviderMetadata(
                if (nameIndex >= 0) cursor.getString(nameIndex)?.take(MAX_DISPLAY_NAME_CHARS) else null
            )
        }
    }.getOrNull() ?: ProviderMetadata(null)

    companion object {
        const val MAX_ATTACHMENT_BYTES = 100L * 1024L * 1024L
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
        private const val DEFAULT_EXTENSION = "bin"
        private const val MAX_DISPLAY_NAME_CHARS = 255
        private val SAFE_EXTENSION = Regex("[A-Za-z0-9]{1,10}")
        private val UUID_PATTERN = Regex("[0-9a-fA-F-]{36}")
        private val UNSAFE_DISPLAY_NAME = Regex("[^A-Za-z0-9._() -]")
    }
}

class AttachmentTooLargeException(val maximumBytes: Long) : IOException()
