package com.example.memogotchi.ui.page

import android.content.Context

object MemoStore {
    private const val PREFS = "memogotchi_identity"
    private const val KEY_NAME = "pet_name"

    fun loadName(context: Context): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_NAME, null)
    fun saveName(context: Context, name: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_NAME, name).apply()
}
