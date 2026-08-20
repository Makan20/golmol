package com.example.nargesapp.ui.utils

import android.content.Context
import android.content.SharedPreferences

object LockPreferences {
    private const val PREFS_NAME = "narges_lock_prefs"
    private const val KEY_LOCK_ENABLED = "lock_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }
}