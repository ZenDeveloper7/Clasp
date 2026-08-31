package com.zen.clasp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.DeletionState
import com.zen.clasp.model.ExtractionState
import com.zen.clasp.model.ProcessingState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureDaoTest {
    private lateinit var database: ClaspDatabase
    private lateinit var dao: CaptureDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClaspDatabase::class.java
        ).build()
        dao = database.captureDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertUpdateAndDelete_preservesTheCaptureGraph() = runBlocking {
        val capture = captureEntity()
        val attachment = attachmentEntity(capture.id)

        dao.insert(capture, attachment)
        dao.insert(
            capture.copy(
                id = "00000000-0000-0000-0000-000000000003",
                originalText = "Second indexed capture"
            ),
            attachment = null
        )
        val stored = dao.observeAll().first().first { it.capture.id == capture.id }
        assertEquals(capture, stored.capture)
        assertEquals(listOf(attachment), stored.attachments)

        dao.updateFavorite(capture.id, favorite = true, updatedAt = 2)
        assertEquals(true, dao.getById(capture.id)?.capture?.isFavorite)

        dao.updateEditableFields(capture.id, "Searchable title", null, updatedAt = 3)
        assertEquals("Searchable title", dao.getById(capture.id)?.capture?.userTitle)

        dao.deleteById(capture.id)
        dao.deleteById("00000000-0000-0000-0000-000000000003")
        assertNull(dao.getById(capture.id))
        database.query("SELECT COUNT(*) FROM attachments", null).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    private fun captureEntity() = CaptureEntity(
        id = "00000000-0000-0000-0000-000000000001",
        type = CaptureType.FILE.name,
        createdAt = 1,
        updatedAt = 1,
        sourcePackage = null,
        originalText = null,
        userTitle = null,
        userNote = null,
        isFavorite = false,
        processingState = ProcessingState.STORED.name,
        extractedText = null,
        extractionState = ExtractionState.NOT_APPLICABLE.name,
        extractionErrorCode = null,
        contentRevision = 1,
        deletionState = DeletionState.ACTIVE.name,
        errorCode = null
    )

    private fun attachmentEntity(captureId: String) = AttachmentEntity(
        id = "00000000-0000-0000-0000-000000000002",
        captureId = captureId,
        relativePath = "captures/$captureId/payload.bin",
        originalDisplayName = "capture.bin",
        mimeType = "application/octet-stream",
        sizeBytes = 4,
        sha256 = "hash"
    )
}
