package com.example.watermapp.data

import android.content.Context

object LoginPrefs {

    fun saveLogin(context: Context, isStaff: Boolean) {
        val prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_staff", isStaff).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_staff", false)
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}