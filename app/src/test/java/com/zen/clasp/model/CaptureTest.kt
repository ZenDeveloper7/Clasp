package com.zen.clasp.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureTest {
    @Test
    fun displayTitle_prefersUserTitleThenAttachmentThenOriginalText() {
        val capture = capture(
            userTitle = "Edited title",
            originalText = "Original first line\nSecond line",
            attachments = listOf(attachment("provider-name.pdf"))
        )

        assertEquals("Edited title", capture.displayTitle)
        assertEquals("provider-name.pdf", capture.copy(userTitle = null).displayTitle)
        assertEquals(
            "Original first line",
            capture.copy(userTitle = null, attachments = emptyList()).displayTitle
        )
    }

    @Test
    fun displayTitle_usesTypeWhenNoContentIsAvailable() {
        assertEquals("File", capture().displayTitle)
    }

    private fun capture(
        userTitle: String? = null,
        originalText: String? = null,
        attachments: List<Attachment> = emptyList()
    ) = Capture(
        id = "id",
        type = CaptureType.FILE,
        createdAt = 0,
        updatedAt = 0,
        sourcePackage = null,
        originalText = originalText,
        userTitle = userTitle,
        userNote = null,
        isFavorite = false,
        processingState = ProcessingState.STORED,
        deletionState = DeletionState.ACTIVE,
        errorCode = null,
        attachments = attachments
    )

    private fun attachment(displayName: String) = Attachment(
        id = "attachment",
        relativePath = "captures/id/payload.pdf",
        originalDisplayName = displayName,
        mimeType = "application/pdf",
        sizeBytes = 1,
        sha256 = "hash"
    )
}
