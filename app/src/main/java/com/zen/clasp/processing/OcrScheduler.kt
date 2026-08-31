package com.zen.clasp.processing

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

interface OcrScheduler {
    fun schedule(captureId: String, replace: Boolean = false)
    fun cancel(captureId: String)
}

class WorkManagerOcrScheduler(context: Context) : OcrScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedule(captureId: String, replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<OcrWorker>()
            .setInputData(workDataOf(OcrWorker.CAPTURE_ID to captureId))
            .addTag(OCR_TAG)
            .build()
        workManager.enqueueUniqueWork(
            workName(captureId),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun cancel(captureId: String) {
        workManager.cancelUniqueWork(workName(captureId))
    }

    private fun workName(captureId: String) = "clasp-ocr-$captureId"

    companion object {
        private const val OCR_TAG = "clasp-ocr"
    }
}
