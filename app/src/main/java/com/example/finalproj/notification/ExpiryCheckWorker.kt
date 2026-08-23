package com.example.finalproj.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finalproj.data.AppDatabase

class ExpiryCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val db = AppDatabase.getDatabase(applicationContext)

        // Get all documents to ensure their notification alarms are up to date
        val documents = db.documentDao().getAllDocumentsForWorker()

        for (doc in documents) {
            //  multi-window scheduler
            if (doc.notificationEnabled) {
                AlarmScheduler.scheduleAlarmsForDocument(applicationContext, doc)
            }
        }

        return Result.success()
    }
}