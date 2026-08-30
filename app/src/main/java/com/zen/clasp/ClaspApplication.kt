package com.zen.clasp

import android.app.Application
import com.zen.clasp.data.ClaspDatabase
import com.zen.clasp.data.DefaultCaptureRepository
import com.zen.clasp.data.FileAttachmentStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClaspApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository by lazy {
        DefaultCaptureRepository(
            captureDao = ClaspDatabase.create(this).captureDao(),
            attachmentStore = FileAttachmentStore(this),
            contentResolver = contentResolver
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.resumeInterruptedDeletions()
        }
    }
}
