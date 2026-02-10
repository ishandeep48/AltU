package com.electron48.altu

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var prefsManager: PrefsManager

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        prefsManager = PrefsManager(this)

        val rgTheme = findViewById<RadioGroup>(R.id.rgTheme)
        val llTransparency = findViewById<LinearLayout>(R.id.llTransparency)
        val sbTransparency = findViewById<SeekBar>(R.id.sbTransparency)
        val tvTransparencyValue = findViewById<TextView>(R.id.tvTransparencyValue)
        val btnAddWidget = findViewById<Button>(R.id.btnAddWidget)

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbTranslucent) {
                llTransparency.visibility = View.VISIBLE
            } else {
                llTransparency.visibility = View.GONE
            }
        }

        sbTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTransparencyValue.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnAddWidget.setOnClickListener {
            val context = this@WidgetConfigActivity

            // Save preferences
            val isMaterialYou = rgTheme.checkedRadioButtonId == R.id.rbMaterialYou
            val transparency = sbTransparency.progress
            val isDarkTint = findViewById<RadioGroup>(R.id.rgTint).checkedRadioButtonId == R.id.rbTintDark
            
            saveWidgetPref(context, appWidgetId, isMaterialYou, transparency, isDarkTint)

            // It is the responsibility of the configuration activity to update the app widget
            val appWidgetManager = AppWidgetManager.getInstance(context)
            MenuWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)

            // Make sure we pass back the original appWidgetId
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    companion object {
        private const val PREFS_NAME = "com.electron48.altu.WidgetProvider"
        private const val PREF_PREFIX_KEY = "appwidget_"

        internal fun saveWidgetPref(context: Context, appWidgetId: Int, isMaterialYou: Boolean, transparency: Int, isDarkTint: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + "_material", isMaterialYou)
            prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_transparency", transparency)
            prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + "_dark_tint", isDarkTint)
            prefs.apply()
        }

        internal fun loadWidgetPref(context: Context, appWidgetId: Int): Triple<Boolean, Int, Boolean> {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val isMaterialYou = prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + "_material", false)
            val transparency = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_transparency", 50)
            val isDarkTint = prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + "_dark_tint", true)
            return Triple(isMaterialYou, transparency, isDarkTint)
        }
        
        internal fun deleteWidgetPref(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_material")
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_transparency")
            prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_dark_tint")
            prefs.apply()
        }
        
        // Helper to save current meal index
        internal fun saveWidgetMealIndex(context: Context, appWidgetId: Int, index: Int) {
             val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
             prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_index", index)
             prefs.apply()
        }
        
        internal fun loadWidgetMealIndex(context: Context, appWidgetId: Int): Int {
             val prefs = context.getSharedPreferences(PREFS_NAME, 0)
             return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_index", 1) // Default to 1 (Lunch) as it's often the next meal
        }
    }
}
