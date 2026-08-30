package com.zen.clasp.data

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileAttachmentStoreTest {
    @Test
    fun copyFrom_ignoresProviderFilenameForOwnedStoragePath() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = FileAttachmentStore(context)
        val captureId = "00000000-0000-0000-0000-000000000010"

        try {
            val stored = store.copyFrom(
                resolver = context.contentResolver,
                uri = Uri.parse("content://com.zen.clasp.test.content/item"),
                captureId = captureId,
                declaredMimeType = "image/png"
            )

            assertEquals("../../clasp.db", stored.originalDisplayName)
            assertTrue(stored.relativePath.startsWith("captures/$captureId/payload."))
            assertFalse(stored.relativePath.contains(".."))
            assertArrayEquals(
                TestContentProvider.CONTENT,
                File(context.filesDir, stored.relativePath).readBytes()
            )
        } finally {
            store.deleteCapture(captureId)
        }
    }
}
