package com.zen.clasp

import android.app.Application
import com.zen.clasp.data.ClaspDatabase
import com.zen.clasp.data.DefaultCaptureRepository
import com.zen.clasp.data.FileAttachmentStore
import com.zen.clasp.processing.MlKitOcrEngine
import com.zen.clasp.processing.WorkManagerOcrScheduler
import com.zen.clasp.search.AppSearchCaptureIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClaspApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val attachmentStore by lazy { FileAttachmentStore(this) }

    val repository by lazy {
        DefaultCaptureRepository(
            captureDao = ClaspDatabase.create(this).captureDao(),
            attachmentStore = attachmentStore,
            contentResolver = contentResolver,
            ocrEngine = MlKitOcrEngine(this),
            ocrScheduler = WorkManagerOcrScheduler(this),
            searchIndex = AppSearchCaptureIndex(this)
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.resumeInterruptedDeletions()
            repository.enqueuePendingOcr()
            runCatching { repository.synchronizeSearchIndex() }
        }
    }
}
