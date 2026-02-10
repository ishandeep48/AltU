package com.electron48.altu

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply dynamic colors if enabled (so this activity itself looks right)
        if (PrefsManager(this).isDynamicTheme()) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
        
        setContentView(R.layout.activity_settings)

        val switchDynamicTheme = findViewById<MaterialSwitch>(R.id.switchDynamicTheme)
        val prefsManager = PrefsManager(this)

        if (!DynamicColors.isDynamicColorAvailable()) {
            switchDynamicTheme.isEnabled = false
            switchDynamicTheme.text = "Material You Not Available (Requires Android 12+)"
        } else {
            switchDynamicTheme.isChecked = prefsManager.isDynamicTheme()
        }

        switchDynamicTheme.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setDynamicTheme(isChecked)
            Toast.makeText(this, "Restarting to apply theme...", Toast.LENGTH_SHORT).show()
            
            // Restart App to apply changes
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }
}
