package com.zen.clasp.processing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zen.clasp.ClaspApplication
import com.zen.clasp.data.OcrRunResult

class OcrWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val captureId = inputData.getString(CAPTURE_ID) ?: return Result.failure()
        val application = applicationContext as? ClaspApplication ?: return Result.failure()
        return when (application.repository.runOcr(captureId)) {
            OcrRunResult.SUCCESS -> Result.success()
            OcrRunResult.RETRYABLE_FAILURE -> {
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
            OcrRunResult.PERMANENT_FAILURE -> Result.failure()
        }
    }

    companion object {
        const val CAPTURE_ID = "capture_id"
        private const val MAX_RETRIES = 2
    }
}
