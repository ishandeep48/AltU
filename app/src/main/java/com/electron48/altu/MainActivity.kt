package com.electron48.altu

import android.os.Bundle
import android.util.Base64
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.widget.Button>(R.id.btnLogin).setOnClickListener {
            val email = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail).text.toString()
            val password = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword).text.toString()

            Thread {
                try {
                    // Base64 encode email and password as required by the endpoint
                    val encodedEmail = Base64.encodeToString(email.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                    val encodedPassword = Base64.encodeToString(password.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)

                    val url = java.net.URL("https://student.bennetterp.camu.in/login/validate")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Appversion", "v2")
                    conn.setRequestProperty("Accept", "application/json, text/plain, */*")
                    conn.setRequestProperty("Origin", "https://student.bennetterp.camu.in")
                    conn.setRequestProperty("Referer", "https://student.bennetterp.camu.in/v2/")
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                    conn.doOutput = true

                    val jsonInputString = "{\"dtype\":\"M\",\"Email\":\"$encodedEmail\",\"pwd\":\"$encodedPassword\",\"isEncr\":true}"
                    
                    conn.outputStream.use { os ->
                        val input = jsonInputString.toByteArray(StandardCharsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = conn.responseCode
                    println("Response Code: $responseCode")
                    
                    val responseBody = if (responseCode in 200..299) {
                        conn.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        null
                    }

                    runOnUiThread {
                       if (responseCode in 200..299 && responseBody != null) {
                           try {
                               val jsonObject = org.json.JSONObject(responseBody)
                               val data = jsonObject.getJSONObject("output").getJSONObject("data")
                               val loginDetails = data.getJSONObject("logindetails")
                               val studentArray = loginDetails.getJSONArray("Student")
                               
                               val userName = if (studentArray.length() > 0) {
                                   val student = studentArray.getJSONObject(0)
                                   
                                   // Save Auth Data
                                   val stuId = student.optString("StuID") // Use optString to avoid crash
                                   val inId = loginDetails.optString("InId").ifEmpty { student.optString("InId") } // Try root first
                                   
                                   // Extract Set-Cookie header - improved logic
                                   // Some servers send multiple Set-Cookie headers. 
                                   // HttpURLConnection might return the last one or we need headerFields for all.
                                   // For now, let's look for connect.sid specifically.
                                   
                                   var connectSid: String? = null
                                   val headers = conn.headerFields
                                   val cookies = headers["Set-Cookie"]
                                   
                                   if (cookies != null) {
                                       for (cookie in cookies) {
                                           if (cookie.contains("connect.sid")) {
                                               // Extract just the value: connect.sid=...; Path=/; HttpOnly...
                                               // We want the whole string up to the first semicolon usually, 
                                               // but for passing back as 'Cookie' header, sending the raw 'connect.sid=XYZ' is best.
                                               val parts = cookie.split(";")
                                               for (part in parts) {
                                                   if (part.trim().startsWith("connect.sid")) {
                                                       connectSid = part.trim().substringAfter("connect.sid=")
                                                       break
                                                   }
                                               }
                                           }
                                           if (connectSid != null) break
                                       }
                                   }

                                   if (connectSid != null) {
                                       val prefsManager = PrefsManager(this@MainActivity)
                                       prefsManager.saveAuthData(connectSid, stuId, inId)
                                       println("Saved Session: $connectSid, Stu: $stuId, In: $inId") 
                                   } else {
                                       println("WARNING: connect.sid not found in cookies: $cookies")
                                   }

                                   "${student.getString("FNa")} ${student.getString("LNa")}".trim()
                               } else {
                                   loginDetails.getString("Name")
                               }

                               android.widget.Toast.makeText(this@MainActivity, "Login Successful", android.widget.Toast.LENGTH_SHORT).show()
                               
                               val intent = android.content.Intent(this@MainActivity, HomeActivity::class.java)
                               intent.putExtra("USER_NAME", userName)
                               startActivity(intent)
                               // finish() // Optional: close login activity
                           } catch (e: Exception) {
                               e.printStackTrace()
                               android.widget.Toast.makeText(this@MainActivity, "Parsing Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                           }
                       } else {
                           android.widget.Toast.makeText(this@MainActivity, "Login Failed: $responseCode", android.widget.Toast.LENGTH_SHORT).show()
                       }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                     runOnUiThread {
                       android.widget.Toast.makeText(this@MainActivity, "Request Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }
}