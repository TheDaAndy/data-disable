package com.example.disabledata

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenUnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val serviceIntent = Intent(context, DataDisablerService::class.java)
            serviceIntent.putExtra("cancelTimer", true)
            context.startService(serviceIntent)
        }
    }
}