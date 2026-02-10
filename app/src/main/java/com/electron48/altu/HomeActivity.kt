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
import com.google.android.material.navigation.NavigationView

class HomeActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        findViewById<TextView>(R.id.tvUserName).text = userName

        // Setup Toolbar
        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // Setup Navigation View
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_todays_menu -> {
                    val intent = Intent(this, MenuActivity::class.java)
                    startActivity(intent)
                    drawerLayout.close()
                    true
                }
                R.id.nav_logout -> {
                    PrefsManager(this).clearAll()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
