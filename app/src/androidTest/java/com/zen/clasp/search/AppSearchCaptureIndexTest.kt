package com.zen.clasp.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.DeletionState
import com.zen.clasp.model.ExtractionState
import com.zen.clasp.model.ProcessingState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSearchCaptureIndexTest {
    @Test
    fun rebuildSearchAndRemove_usePrivateLocalIndex() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val index = AppSearchCaptureIndex(context)
        val capture = capture("capture-1", "Project Atlas receipt")

        try {
            index.rebuild(listOf(capture))

            val result = index.search("proj atl").single()
            assertEquals(capture.id, result.captureId)
            assertEquals(SearchField.TITLE, result.matchedField)
            assertTrue(result.excerpt.contains("Project", ignoreCase = true))

            index.remove(capture.id)
            assertTrue(index.search("project").isEmpty())
        } finally {
            index.rebuild(emptyList())
        }
    }

    private fun capture(id: String, title: String) = Capture(
        id = id,
        type = CaptureType.TEXT,
        createdAt = 1,
        updatedAt = 1,
        sourcePackage = null,
        originalText = null,
        userTitle = title,
        userNote = null,
        isFavorite = false,
        processingState = ProcessingState.STORED,
        extractedText = null,
        extractionState = ExtractionState.NOT_APPLICABLE,
        extractionErrorCode = null,
        contentRevision = 1,
        deletionState = DeletionState.ACTIVE,
        errorCode = null,
        attachments = emptyList()
    )
}
