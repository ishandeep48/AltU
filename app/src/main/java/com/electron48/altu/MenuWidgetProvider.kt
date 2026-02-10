package com.electron48.altu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MenuWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.electron48.altu.ACTION_REFRESH"
        const val ACTION_PREV = "com.electron48.altu.ACTION_PREV"
        const val ACTION_NEXT = "com.electron48.altu.ACTION_NEXT"
        const val EXTRA_WIDGET_ID = "com.electron48.altu.EXTRA_WIDGET_ID" // Correct generic extra for ID usage inside intent if needed, though AppWidgetManager uses its own.

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefsManager = PrefsManager(context)
            val (cachedMenu, cachedDate) = prefsManager.getMenu()
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // Load Preferences
            val (isMaterialYou, transparency, isDarkTint) = WidgetConfigActivity.loadWidgetPref(context, appWidgetId)
            val currentMealIndex = WidgetConfigActivity.loadWidgetMealIndex(context, appWidgetId) // 0=Breakfast, 1=Lunch, 2=Snacks, 3=Dinner

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_menu)
            
            // Apply Theme
            if (isMaterialYou) {
                 views.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.widget_background_material)
            } else {
                // Translucent
                val alpha = (transparency * 255) / 100
                val color = if (isDarkTint) {
                    Color.argb(alpha, 0, 0, 0)
                } else {
                    // Light Glass (Frosted)
                    Color.argb(alpha, 255, 255, 255)
                }
                views.setInt(R.id.widgetRoot, "setBackgroundColor", color)
                
                // Adjust Text & Icon Color based on Tint
                 if (isDarkTint) {
                     val textColor = Color.WHITE
                     views.setTextColor(R.id.tvWidgetTitle, textColor)
                     views.setTextColor(R.id.tvMealName, textColor)
                     views.setTextColor(R.id.tvMealItems, textColor)
                     
                     views.setInt(R.id.btnRefresh, "setColorFilter", textColor)
                     views.setInt(R.id.btnPrev, "setColorFilter", textColor)
                     views.setInt(R.id.btnNext, "setColorFilter", textColor)
                 } else {
                     val textColor = Color.BLACK
                     views.setTextColor(R.id.tvWidgetTitle, textColor)
                     views.setTextColor(R.id.tvMealName, textColor)
                     views.setTextColor(R.id.tvMealItems, textColor)
                     
                     views.setInt(R.id.btnRefresh, "setColorFilter", textColor)
                     views.setInt(R.id.btnPrev, "setColorFilter", textColor)
                     views.setInt(R.id.btnNext, "setColorFilter", textColor)
                 }
            }
            
            // Intent to launch app on title click
            val intentApp = Intent(context, MenuActivity::class.java)
            val pendingIntentApp = PendingIntent.getActivity(context, 0, intentApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tvWidgetTitle, pendingIntentApp)
            
            // Refresh Intent
            val refreshIntent = Intent(context, MenuWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPending = PendingIntent.getBroadcast(context, appWidgetId * 10 + 2, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btnRefresh, refreshPending)


            // Navigation Intents
            val prevIntent = Intent(context, MenuWidgetProvider::class.java).apply {
                action = ACTION_PREV
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val prevPending = PendingIntent.getBroadcast(context, appWidgetId * 10, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btnPrev, prevPending)

            val nextIntent = Intent(context, MenuWidgetProvider::class.java).apply {
                action = ACTION_NEXT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPending = PendingIntent.getBroadcast(context, appWidgetId * 10 + 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btnNext, nextPending)


            
            // Data Loading
            val meals = arrayOf("Breakfast", "Lunch", "Snacks", "Dinner")
            val currentMealName = meals[currentMealIndex]
            
            views.setTextViewText(R.id.tvMealName, currentMealName)

            if (cachedMenu != null && cachedDate == todayDate) {
                 try {
                    val jsonObject = JSONObject(cachedMenu)
                    val data = jsonObject.getJSONObject("output").getJSONObject("data")
                    val mealList = data.getJSONArray("oMealList")

                    var mealContent = "Not Available"

                    for (i in 0 until mealList.length()) {
                        val meal = mealList.getJSONObject(i)
                        val mealName = meal.getString("msCde")
                        val mealItems = meal.getString("msNme")

                        // Check if this API item matches our current selected meal
                        if (mealName.contains(currentMealName, ignoreCase = true)) { 
                             // Clean for widget: remove (...) then remove trailing dashes line-by-line
                             mealContent = mealItems.lineSequence()
                                 .map { it.replace(Regex("\\(.*\\)"), "").replace(Regex("[-]+\\s*$"), "").trim() }
                                 .filter { it.isNotBlank() }
                                 .joinToString("\n")
                             break
                        } else if (currentMealName == "Snacks" && mealName.contains("Snack", ignoreCase = true)) {
                             mealContent = mealItems.lineSequence()
                                 .map { it.replace(Regex("\\(.*\\)"), "").replace(Regex("[-]+\\s*$"), "").trim() }
                                 .filter { it.isNotBlank() }
                                 .joinToString("\n")
                             break
                        }
                    }
                    
                    views.setTextViewText(R.id.tvMealItems, mealContent)

                } catch (e: Exception) {
                    e.printStackTrace()
                    views.setTextViewText(R.id.tvMealItems, "Error loading data")
                }
            } else {
                views.setTextViewText(R.id.tvMealItems, "Tap 'Overview' to Refresh")
            }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetConfigActivity.deleteWidgetPref(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        
        if (action == ACTION_PREV || action == ACTION_NEXT) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                var currentIndex = WidgetConfigActivity.loadWidgetMealIndex(context, widgetId)
                
                if (action == ACTION_PREV) {
                    currentIndex = (currentIndex - 1 + 4) % 4
                } else {
                    currentIndex = (currentIndex + 1) % 4
                }
                
                WidgetConfigActivity.saveWidgetMealIndex(context, widgetId, currentIndex)
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
        else if (action == ACTION_REFRESH) {
             val pendingResult = goAsync()
             val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
             
             Thread {
                 try {
                     val prefsManager = PrefsManager(context)
                     val (cookie, stuId, inId) = prefsManager.getAuthData()
                     
                     if (cookie != null && stuId != null && inId != null) {
                         val dayOfWeek = SimpleDateFormat("EEE", Locale.US).format(Date()).uppercase()
                         val url = java.net.URL("https://student.bennetterp.camu.in/api/mess-management/get-student-menu-list")
                         val conn = url.openConnection() as java.net.HttpURLConnection
                         conn.requestMethod = "POST"
                         conn.setRequestProperty("Content-Type", "application/json")
                         conn.setRequestProperty("Cookie", "connect.sid=$cookie")
                         conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                         conn.doOutput = true

                         val jsonInputString = "{\"stuId\":\"$stuId\",\"InId\":\"$inId\",\"day\":\"$dayOfWeek\"}"
                         conn.outputStream.use { it.write(jsonInputString.toByteArray(java.nio.charset.StandardCharsets.UTF_8)) }
                         
                         if (conn.responseCode in 200..299) {
                             val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                             val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                             prefsManager.saveMenu(responseBody, todayDate)
                         }
                     }
                 } catch (e: Exception) {
                     e.printStackTrace()
                 } finally {
                      val appWidgetManager = AppWidgetManager.getInstance(context)
                      if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                          updateAppWidget(context, appWidgetManager, widgetId)
                      } else {
                          // Update all
                           val appWidgetIds = appWidgetManager.getAppWidgetIds(
                                android.content.ComponentName(context, MenuWidgetProvider::class.java)
                           )
                           onUpdate(context, appWidgetManager, appWidgetIds)
                      }
                      pendingResult.finish()
                 }
             }.start()
        }
        else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
             val appWidgetManager = AppWidgetManager.getInstance(context)
             val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, MenuWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
