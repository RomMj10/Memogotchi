package com.example.memogotchi.ui.page

import android.content.Context

object  XpStore {
    private const val PREFS = "memogotchi_xp"
    private const val KEY_TOTAL_XP = "total_xp"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadXp(context: Context): Int = prefs(context).getInt(KEY_TOTAL_XP, 0)
    fun addXp(context: Context, amount: Int): Int {
        val newTotal = loadXp(context) + amount
        prefs(context).edit().putInt(KEY_TOTAL_XP, newTotal).apply()
        return newTotal
    }
}