package com.electron48.altu

import android.os.Bundle
import android.util.Base64
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MenuActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        prefsManager = PrefsManager(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        loadMenu()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                fetchMenuFromApi()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadMenu() {
        val (cachedMenu, cachedDate) = prefsManager.getMenu()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (cachedMenu != null && cachedDate == todayDate) {
            parseAndDisplayMenu(cachedMenu)
        } else {
            fetchMenuFromApi()
        }
    }

    private fun fetchMenuFromApi() {
        val (cookie, stuId, inId) = prefsManager.getAuthData()

        if (cookie == null || stuId == null || inId == null) {
            Toast.makeText(this, "Session Expired. Please Logout and Login again.", Toast.LENGTH_LONG).show()
            return
        }

        // Show loading state if appropriate, or just wait for response
        // findViewById<TextView>(R.id.tvBreakfast).text = "Loading..." // Views replaced by containers

        Thread {
            try {
                val dayOfWeek = SimpleDateFormat("EEE", Locale.US).format(Date()).uppercase() // e.g., WED

                val url = java.net.URL("https://student.bennetterp.camu.in/api/mess-management/get-student-menu-list")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Cookie", "connect.sid=$cookie") // Send Cookie
                println("Menu Request Cookie: connect.sid=$cookie")
                println("Menu Request Body: {\"stuId\":\"$stuId\",\"InId\":\"$inId\",\"day\":\"$dayOfWeek\"}")
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                conn.doOutput = true

                val jsonInputString = "{\"stuId\":\"$stuId\",\"InId\":\"$inId\",\"day\":\"$dayOfWeek\"}"
                
                conn.outputStream.use { os ->
                   val input = jsonInputString.toByteArray(StandardCharsets.UTF_8)
                   os.write(input, 0, input.size)
                }

                val responseCode = conn.responseCode
                
                if (responseCode in 200..299) {
                    val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                    
                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    prefsManager.saveMenu(responseBody, todayDate)

                    runOnUiThread {
                        parseAndDisplayMenu(responseBody)
                        Toast.makeText(this@MenuActivity, "Menu Refreshed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MenuActivity, "Failed to fetch menu: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MenuActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun parseAndDisplayMenu(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val data = jsonObject.getJSONObject("output").getJSONObject("data")
            
            val curntDte = data.optString("curntDte", "")
            findViewById<TextView>(R.id.tvDate).text = if(curntDte.isNotEmpty()) curntDte.substring(0, 10) else "Today's Menu"

            val mealList = data.getJSONArray("oMealList")

            // Clear existing views
            findViewById<android.widget.LinearLayout>(R.id.llBreakfast).removeAllViews()
            findViewById<android.widget.LinearLayout>(R.id.llLunch).removeAllViews()
            findViewById<android.widget.LinearLayout>(R.id.llSnacks).removeAllViews()
            findViewById<android.widget.LinearLayout>(R.id.llDinner).removeAllViews()

            for (i in 0 until mealList.length()) {
                val meal = mealList.getJSONObject(i)
                val mealName = meal.getString("msCde")
                val mealItems = meal.getString("msNme")

                if (mealName.contains("Breakfast", ignoreCase = true)) {
                    populateMealContainer(findViewById(R.id.llBreakfast), mealItems)
                } else if (mealName.contains("Lunch", ignoreCase = true)) {
                     populateMealContainer(findViewById(R.id.llLunch), mealItems)
                } else if (mealName.contains("Snack", ignoreCase = true)) {
                     populateMealContainer(findViewById(R.id.llSnacks), mealItems)
                } else if (mealName.contains("Dinner", ignoreCase = true)) {
                     populateMealContainer(findViewById(R.id.llDinner), mealItems)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
             Toast.makeText(this, "Error parsing data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateMealContainer(container: android.widget.LinearLayout, mealItemsString: String) {
        val lines = mealItemsString.split("\n")
        val regex = Regex("(.*)\\((.*[kK]cal)\\)") // Captures Name(Cal)

        for (line in lines) {
            if (line.isBlank()) continue
            
            val trimmedLine = line.trim()
            var name = trimmedLine
            var calories = ""

            val matchResult = regex.find(trimmedLine)
            if (matchResult != null) {
                name = matchResult.groupValues[1].trim().removeSuffix("-").trim()
                calories = matchResult.groupValues[2].trim()
            } else {
                // Try simpler split if regex fails but has parens?
                // For now, if no match, just show full line as name
            }

            // Create Row Layout
            val rowLayout = android.widget.LinearLayout(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                orientation = android.widget.LinearLayout.HORIZONTAL
            }

            val tvName = TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = name
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            }

            val tvCalories = TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = calories
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                setPadding(16, 0, 0, 0)
            }

            rowLayout.addView(tvName)
            rowLayout.addView(tvCalories)
            container.addView(rowLayout)
            
            // Add separator
            val separator = android.view.View(this).apply {
                 layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(resources.getColor(android.R.color.darker_gray, theme))
                alpha = 0.2f
            }
            container.addView(separator)
        }
    }
}
