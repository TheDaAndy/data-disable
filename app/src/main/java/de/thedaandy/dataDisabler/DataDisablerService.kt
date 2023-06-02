package de.thedaandy.dataDisabler

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

class DataDisablerService : Service() {

    private val CHANNEL_ID = "DataDisablerServiceChannel"
    private var timer: CountDownTimer? = null

    private var isWifiEnabledBeforeDisable = false
    private var isMobileDataEnabledBeforeDisable = false

    private val screenOffReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val prefs = context.getSharedPreferences("de.thedaandy.data_disabler", Context.MODE_PRIVATE)
            if (intent.action == Intent.ACTION_SCREEN_OFF) {

                // Save the current Wi-Fi and mobile data states
                isWifiEnabledBeforeDisable = isWifiEnabled(context)
                isMobileDataEnabledBeforeDisable = isMobileDataEnabled(context)


                val delayTime = prefs.getLong("delayTime", 0)
                timer = object : CountDownTimer(delayTime * 1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        timer = null
                        if (prefs.getBoolean("wlanDisablerEnabled", false)) {
                            disableWifi()
                        }
                        if (prefs.getBoolean("mobileDataDisablerEnabled", false)) {
                            disableData()
                        }
                    }
                }.start()
            }
        }
    }

    private val screenUnlockReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                timer?.cancel()

                val prefs = context.getSharedPreferences("de.thedaandy.data_disabler", Context.MODE_PRIVATE)
                if (prefs.getBoolean("restoreSettingsEnabled", false)) {
                    // Check if Wi-Fi was enabled before disabling and restore its state
                    if (isWifiEnabledBeforeDisable) {
                        enableWifi()
                    }

                    // Check if mobile data was enabled before disabling and restore its state
                    if (isMobileDataEnabledBeforeDisable) {
                        enableData()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val offFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, offFilter)
        val unlockFilter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(screenUnlockReceiver, unlockFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = applicationContext.getSharedPreferences("de.thedaandy.data_disabler", Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean("wlanDisablerEnabled", false)
                          || prefs.getBoolean("mobileDataDisablerEnabled", false)
        if (!serviceEnabled) {
            killForeground()
            return START_NOT_STICKY
        } else {
            createNotificationChannel()

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Data Disabler")
                .setContentText("Data Disabler Service is running in the foreground to disable data when the screen is off.")
                .setSmallIcon(R.mipmap.app_icon)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(1, notification)

            return START_STICKY
        }
    }

    private fun killForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenOffReceiver)
        unregisterReceiver(screenUnlockReceiver)
        timer?.cancel()
    }


    // Helper method to check if wifi data is enabled
    private fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    // Helper method to check if mobile data is enabled
    private fun isMobileDataEnabled(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return try {
            @SuppressLint("DiscouragedPrivateApi")
            val method = ConnectivityManager::class.java.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true
            method.invoke(connectivityManager) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Data Disabler Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun enableWifi() {
        suAction("svc wifi enable")
    }
    private fun disableWifi() {
        suAction("svc wifi disable")
    }

    private fun disableData() {
        suAction("svc data disable")
    }
    private fun enableData() {
        suAction("svc data enable")
    }

    private fun suAction(suAction: String) {
        try {
            suActionStatic(suAction)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("DataDisabler", "Failed to execute su command: $suAction")
            handleSuFailure()
        }
    }

    private fun handleSuFailure() {
        val prefs = applicationContext.getSharedPreferences("de.thedaandy.data_disabler", Context.MODE_PRIVATE)
        val edit = prefs.edit()
        edit.putBoolean("rootCheckSuccessful", false)
        edit.apply()
        killForeground()
    }

    companion object {
        fun rootAvailable(): Boolean {
            try {
                val result = suActionStatic("id")
                return result.contains("uid=0")
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            return false
        }

        private fun suActionStatic(suAction: String): String {
            Log.d("DataDisabler", "Executing su command: $suAction")
            val su = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(su.outputStream)
            val inputStream = BufferedReader(InputStreamReader(su.inputStream))

            outputStream.writeBytes(suAction + "\n")
            outputStream.flush()

            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val output = StringBuilder()
            var line: String? = inputStream.readLine()
            while (line != null) {
                output.append(line)
                line = inputStream.readLine()
            }

            su.waitFor()
            return output.toString()
        }
    }

}