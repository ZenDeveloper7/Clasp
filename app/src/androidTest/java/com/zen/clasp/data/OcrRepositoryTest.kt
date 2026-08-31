package com.zen.clasp.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zen.clasp.model.ExtractionState
import com.zen.clasp.processing.OcrEngine
import com.zen.clasp.processing.OcrScheduler
import com.zen.clasp.search.AppSearchCaptureIndex
import com.zen.clasp.search.SearchFilters
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OcrRepositoryTest {
    @Test
    fun runOcr_persistsExtractedTextAndMakesItSearchable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, ClaspDatabase::class.java).build()
        val scheduler = RecordingScheduler()
        val repository = DefaultCaptureRepository(
            captureDao = database.captureDao(),
            attachmentStore = FileAttachmentStore(context),
            contentResolver = context.contentResolver,
            ocrEngine = TestOcrEngine { "Coffee receipt total 42" },
            ocrScheduler = scheduler,
            searchIndex = AppSearchCaptureIndex(context),
            now = { 100L }
        )

        val captureId = repository.importUri(
            Uri.parse("content://com.zen.clasp.test.content/ocr"),
            "image/png"
        )
        try {
            assertEquals(listOf(captureId), scheduler.scheduled)
            assertEquals(OcrRunResult.SUCCESS, repository.runOcr(captureId))
            val stored = database.captureDao().getById(captureId)!!.capture
            assertEquals(ExtractionState.COMPLETE.name, stored.extractionState)
            assertEquals("Coffee receipt total 42", stored.extractedText)
            assertEquals(
                captureId,
                repository.search("coffee", SearchFilters()).single().capture.id
            )
        } finally {
            repository.delete(captureId)
            database.close()
        }
    }

    private class RecordingScheduler : OcrScheduler {
        val scheduled = mutableListOf<String>()

        override fun schedule(captureId: String, replace: Boolean) {
            scheduled += captureId
        }

        override fun cancel(captureId: String) = Unit
    }
}

private fun interface TestOcrEngine : OcrEngine {
    override suspend fun recognize(file: File): String
}
