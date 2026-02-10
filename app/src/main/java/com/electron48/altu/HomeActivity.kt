package com.electron48.altu

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.Intent
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar

class HomeActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var gestureDetector: android.view.GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply Dynamic Colors
        if (PrefsManager(this).isDynamicTheme()) {
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        }
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        drawerLayout = findViewById(R.id.drawer_layout)
        val btnMenu = findViewById<android.widget.ImageButton>(R.id.btnMenu)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        findViewById<TextView>(R.id.tvUserName).text = userName

        // Setup Menu Button
        btnMenu.setOnClickListener {
            drawerLayout.open()
        }
        
        // Setup Gesture Detector for "Swipe Right Anywhere"
        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e2.x > e1.x) { // Left to Right
                    val deltaX = e2.x - e1.x
                    val deltaY = Math.abs(e2.y - e1.y)
                    if (deltaX > 100 && deltaX > deltaY && Math.abs(velocityX) > 100) {
                        drawerLayout.open()
                        return true
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })


        // Setup Custom Navigation Drawer items
        findViewById<View>(R.id.nav_todays_menu).setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            drawerLayout.close()
        }

        findViewById<View>(R.id.nav_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            drawerLayout.close()
        }

        findViewById<View>(R.id.nav_logout).setOnClickListener {
            PrefsManager(this).clearAll()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev != null) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }
}
