package de.thedaandy.dataDisabler

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.content.Intent

class BootCompletedWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val serviceIntent = Intent(applicationContext, DataDisablerService::class.java)
        applicationContext.startService(serviceIntent)
        return Result.success()
    }
}