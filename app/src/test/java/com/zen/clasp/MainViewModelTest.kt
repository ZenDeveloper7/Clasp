package com.zen.clasp

import android.net.Uri
import com.zen.clasp.data.CaptureRepository
import com.zen.clasp.data.ExportSpec
import com.zen.clasp.model.Capture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createText_persistsBeforeReportingSuccess() = runTest {
        val repository = FakeCaptureRepository()
        val viewModel = MainViewModel(repository)
        var callbackInvoked = false

        viewModel.createText("Remember this") { callbackInvoked = true }
        advanceUntilIdle()

        assertEquals(listOf("Remember this"), repository.createdTexts)
        assertTrue(callbackInvoked)
        assertEquals("Text saved", viewModel.message.value)
    }

    @Test
    fun createText_failureKeepsUserOnCaptureFlow() = runTest {
        val repository = FakeCaptureRepository(failCreate = true)
        val viewModel = MainViewModel(repository)
        var callbackInvoked = false

        viewModel.createText("Remember this") { callbackInvoked = true }
        advanceUntilIdle()

        assertEquals(false, callbackInvoked)
        assertEquals(
            "The operation failed. Your existing captures are unchanged.",
            viewModel.message.value
        )
    }

    private class FakeCaptureRepository(
        private val failCreate: Boolean = false
    ) : CaptureRepository {
        override val captures: Flow<List<Capture>> = MutableStateFlow(emptyList())
        val createdTexts = mutableListOf<String>()

        override suspend fun createText(text: String, sourcePackage: String?): String {
            if (failCreate) error("storage unavailable")
            createdTexts += text
            return "capture-id"
        }

        override suspend fun importUri(uri: Uri, declaredMimeType: String?, sourcePackage: String?) =
            "capture-id"

        override suspend fun update(captureId: String, title: String?, note: String?) = Unit
        override suspend fun setFavorite(captureId: String, favorite: Boolean) = Unit
        override suspend fun delete(captureId: String) = Unit
        override suspend fun exportSpec(captureId: String) = ExportSpec("text/plain", "capture.txt")
        override suspend fun export(captureId: String, destination: Uri) = Unit
    }
}
