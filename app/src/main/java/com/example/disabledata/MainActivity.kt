import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.example.disabledata.R

class MainActivity : ComponentActivity() {

    private lateinit var serviceSwitch: Switch
    private lateinit var restoreSettingsSwitch: Switch
    private lateinit var delayInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceSwitch = findViewById(R.id.serviceSwitch)
        restoreSettingsSwitch = findViewById(R.id.restoreSettingsSwitch)
        delayInput = findViewById(R.id.delayInput)

        // Restore previous switch states
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        serviceSwitch.isChecked = prefs.getBoolean("serviceEnabled", false)
        restoreSettingsSwitch.isChecked = prefs.getBoolean("restoreSettingsEnabled", false)
        delayInput.setText(prefs.getString("delay", ""))

        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = prefs.edit()
            editor.putBoolean("serviceEnabled", isChecked)
            editor.apply()

            val intent = Intent(this, MyService::class.java)
            if (isChecked) {
                startService(intent)
            } else {
                stopService(intent)
            }
        }

        restoreSettingsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = prefs.edit()
            editor.putBoolean("restoreSettingsEnabled", isChecked)
            editor.apply()
        }

        delayInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val editor = prefs.edit()
                editor.putString("delay", delayInput.text.toString())
                editor.apply()
            }
        }
    }
}