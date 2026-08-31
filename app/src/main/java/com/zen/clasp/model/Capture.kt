package com.zen.clasp.model

data class Capture(
    val id: String,
    val type: CaptureType,
    val createdAt: Long,
    val updatedAt: Long,
    val sourcePackage: String?,
    val originalText: String?,
    val userTitle: String?,
    val userNote: String?,
    val isFavorite: Boolean,
    val processingState: ProcessingState,
    val extractedText: String?,
    val extractionState: ExtractionState,
    val extractionErrorCode: String?,
    val contentRevision: Int,
    val deletionState: DeletionState,
    val errorCode: String?,
    val attachments: List<Attachment>
) {
    val displayTitle: String
        get() = userTitle?.takeIf(String::isNotBlank)
            ?: attachments.firstOrNull()?.originalDisplayName?.takeIf(String::isNotBlank)
            ?: originalText?.lineSequence()?.firstOrNull()?.take(80)?.takeIf(String::isNotBlank)
            ?: type.displayName

    val previewText: String?
        get() = userNote?.takeIf(String::isNotBlank)
            ?: originalText?.takeIf(String::isNotBlank)
            ?: extractedText?.takeIf(String::isNotBlank)
            ?: attachments.firstOrNull()?.mimeType
}

data class Attachment(
    val id: String,
    val relativePath: String,
    val originalDisplayName: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String
)

enum class CaptureType(val displayName: String) {
    TEXT("Text"),
    IMAGE("Image"),
    FILE("File");

    companion object {
        fun fromStorage(value: String) = entries.firstOrNull { it.name == value } ?: FILE
    }
}

enum class ProcessingState {
    STORED;

    companion object {
        fun fromStorage(value: String) = entries.firstOrNull { it.name == value } ?: STORED
    }
}

enum class ExtractionState {
    NOT_APPLICABLE,
    PENDING,
    RUNNING,
    COMPLETE,
    EMPTY,
    FAILED;

    companion object {
        fun fromStorage(value: String) = entries.firstOrNull { it.name == value } ?: NOT_APPLICABLE
    }
}

enum class DeletionState {
    ACTIVE,
    DELETING,
    FAILED;

    companion object {
        fun fromStorage(value: String) = entries.firstOrNull { it.name == value } ?: ACTIVE
    }
}
