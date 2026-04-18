package com.firebill.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.firebill.android.data.repository.BillRepository
import java.io.File

class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = BillRepository(applicationContext)

        val pending = repository.getPendingOnce()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false

        for (upload in pending) {
            val file = File(upload.filePath)
            if (!file.exists()) {
                // File gone, remove from queue
                repository.deletePendingUpload(upload)
                continue
            }

            repository.uploadBill(file)
                .onSuccess {
                    repository.deletePendingUpload(upload)
                }
                .onFailure { error ->
                    repository.incrementRetry(upload.id, error.message)
                    if (upload.retryCount >= MAX_RETRIES) {
                        // Give up after too many retries
                        repository.deletePendingUpload(upload)
                    }
                    anyFailed = true
                }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val MAX_RETRIES = 5
    }
}
