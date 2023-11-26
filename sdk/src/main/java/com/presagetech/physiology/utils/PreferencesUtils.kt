package com.presagetech.physiology.utils

import android.content.Context
import com.presagetech.physiology.R

object PreferencesUtils {
    const val Tutorial_Key = "tutorial_has_been_shown"

    fun saveBoolean(context: Context, key: String, boolean: Boolean) {
        val pref = context.getSharedPreferences(
            context.getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )
        pref.edit().apply {
            putBoolean(key, boolean)
            apply()
        }
    }

    fun getBoolean(context: Context, key: String, default: Boolean): Boolean {
        val pref = context.getSharedPreferences(
            context.getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )
        return pref.getBoolean(key, default)
    }
}
