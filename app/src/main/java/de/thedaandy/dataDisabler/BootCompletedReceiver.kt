package de.thedaandy.dataDisabler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val workRequest = OneTimeWorkRequest.Builder(BootCompletedWorker::class.java).build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}