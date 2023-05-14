package de.thedaandy.dataDisabler

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Switch
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var mobileDataSwitch: Switch
    private lateinit var wlanSwitch: Switch
    private lateinit var restoreSettingsSwitch: Switch
    private lateinit var delayTimeInput: EditText

    private var checkingForRoot = false

    private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "rootCheckSuccessful" && !sharedPreferences.getBoolean("rootCheckSuccessful", false)) {
            checkForRoot()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mobileDataSwitch = findViewById(R.id.serviceSwitchMobileData)
        wlanSwitch = findViewById(R.id.serviceSwitchWLAN)
        restoreSettingsSwitch = findViewById(R.id.restoreSettingsSwitch)

        // Retrieve the initial value from shared preferences
        val prefs = getSharedPreferences("de.thedaandy.data_disabler", Context.MODE_PRIVATE)
        val initialDelayTime = prefs.getLong("delayTime", 0)
        // Initialize the delay time input and set the initial value
        delayTimeInput = findViewById(R.id.delayTimeInput)
        delayTimeInput.setText(initialDelayTime.toString())

        // Restore previous switch states
        wlanSwitch.isChecked = prefs.getBoolean("wlanDisablerEnabled", false)
        mobileDataSwitch.isChecked = prefs.getBoolean("mobileDataDisablerEnabled", false)
        restoreSettingsSwitch.isChecked = prefs.getBoolean("restoreSettingsEnabled", false)
        wlanSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = prefs.edit()
            editor.putBoolean("wlanDisablerEnabled", isChecked)
            editor.apply()
            val intent = Intent(this, DataDisablerService::class.java)
            startService(intent)
        }

        mobileDataSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = prefs.edit()
            editor.putBoolean("mobileDataDisablerEnabled", isChecked)
            editor.apply()
            val intent = Intent(this, DataDisablerService::class.java)
            startService(intent)
        }

        restoreSettingsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = prefs.edit()
            editor.putBoolean("restoreSettingsEnabled", isChecked)
            editor.apply()
        }

        // Create a TextWatcher to detect changes in the input field
        val delayTimeWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Save the input value when it changes
                val editor = prefs.edit()
                editor.putLong("delayTime", s.toString().toLongOrNull() ?: 0)
                editor.apply()
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed for this case
            }
        }

        // Set the TextWatcher on the input field
        delayTimeInput.addTextChangedListener(delayTimeWatcher)

        checkForRoot()

        // Register the SharedPreferences listener
        prefs.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

    override fun onResume() {
        super.onResume()
        checkForRoot()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister the SharedPreferences listener to avoid memory leaks
        val prefs = getSharedPreferences("de.thedaandy.data_disabler", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }


    private fun checkForRoot() {
        if (checkingForRoot) {
            return
        }
        val prefs = getSharedPreferences("de.thedaandy.data_disabler", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("rootCheckSuccessful", false)) {
            checkingForRoot = true
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Requesting Root Access")
            alertDialogBuilder.setMessage("Root access is required to disable/enable mobile data and WLAN. Please grant root access to this app for the following request.")
            alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (DataDisablerService.rootAvailable()) {
                    val edit = prefs.edit()
                    edit.putBoolean("rootCheckSuccessful", true)
                    edit.apply()
                    checkingForRoot = false
                } else {
                    showNoRootPopup()
                    checkingForRoot = false
                }
            }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
        } else {
            onSuccessfulRootAccessCheck()
        }
    }

    private fun onSuccessfulRootAccessCheck() {
        try {
            val intent = Intent(this, DataDisablerService::class.java)
            startService(intent)
        } catch (e: Exception) {
            Log.d("MainActivity", "Could not start DataDisablerService")
        }
    }

    private fun showNoRootPopup() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Root Access Required")
        alertDialogBuilder.setMessage("Root access is required to use this app. Please make sure your device is rooted. Closing App now.")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}