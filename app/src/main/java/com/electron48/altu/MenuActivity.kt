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

        findViewById<TextView>(R.id.tvBreakfast).text = "Loading..."
        findViewById<TextView>(R.id.tvLunch).text = "Loading..."
        findViewById<TextView>(R.id.tvSnacks).text = "Loading..."
        findViewById<TextView>(R.id.tvDinner).text = "Loading..."

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

            // Reset texts
             findViewById<TextView>(R.id.tvBreakfast).text = "Not Available"
             findViewById<TextView>(R.id.tvLunch).text = "Not Available"
             findViewById<TextView>(R.id.tvSnacks).text = "Not Available"
             findViewById<TextView>(R.id.tvDinner).text = "Not Available"

            for (i in 0 until mealList.length()) {
                val meal = mealList.getJSONObject(i)
                val mealName = meal.getString("msCde")
                val mealItems = meal.getString("msNme")

                if (mealName.contains("Breakfast", ignoreCase = true)) {
                    findViewById<TextView>(R.id.tvBreakfast).text = mealItems
                } else if (mealName.contains("Lunch", ignoreCase = true)) {
                     findViewById<TextView>(R.id.tvLunch).text = mealItems
                } else if (mealName.contains("Snack", ignoreCase = true)) {
                     findViewById<TextView>(R.id.tvSnacks).text = mealItems
                } else if (mealName.contains("Dinner", ignoreCase = true)) {
                     findViewById<TextView>(R.id.tvDinner).text = mealItems
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
             findViewById<TextView>(R.id.tvBreakfast).text = "Error parsing data"
        }
    }
}
