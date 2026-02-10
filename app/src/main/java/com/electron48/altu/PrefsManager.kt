package com.electron48.altu

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AltuPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_COOKIE = "auth_cookie"
        const val KEY_STU_ID = "stu_id"
        const val KEY_IN_ID = "in_id"
        const val KEY_CACHED_MENU = "cached_menu"
        const val KEY_MENU_DATE = "menu_date"
    }

    fun saveAuthData(cookie: String, stuId: String, inId: String) {
        prefs.edit().apply {
            putString(KEY_COOKIE, cookie)
            putString(KEY_STU_ID, stuId)
            putString(KEY_IN_ID, inId)
            apply()
        }
    }

    fun getAuthData(): Triple<String?, String?, String?> {
        val cookie = prefs.getString(KEY_COOKIE, null)
        val stuId = prefs.getString(KEY_STU_ID, null)
        val inId = prefs.getString(KEY_IN_ID, null)
        return Triple(cookie, stuId, inId)
    }

    fun saveMenu(menuJson: String, date: String) {
        prefs.edit().apply {
            putString(KEY_CACHED_MENU, menuJson)
            putString(KEY_MENU_DATE, date)
            apply()
        }
    }

    fun getMenu(): Pair<String?, String?> {
        val menu = prefs.getString(KEY_CACHED_MENU, null)
        val date = prefs.getString(KEY_MENU_DATE, null)
        return Pair(menu, date)
    }

    fun clearMenu() {
        prefs.edit().apply {
            remove(KEY_CACHED_MENU)
            remove(KEY_MENU_DATE)
            apply()
        }
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
