package com.example.nargesapp.ui.utils

import android.content.Context

object ReportPreferences {
    private const val PREFS_NAME = "report_prefs"
    private const val KEY_SHOW_AMOUNT = "show_amount_instead_of_percent"

    fun isAmountModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_AMOUNT, false)
    }

    fun setAmountMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_AMOUNT, enabled).apply()
    }
}
