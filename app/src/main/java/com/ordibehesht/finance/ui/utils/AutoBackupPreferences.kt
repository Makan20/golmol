package com.ordibehesht.finance.ui.utils

import android.content.Context

object AutoBackupPreferences {
    private const val PREFS_NAME = "auto_backup_prefs"
    private const val KEY_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST_BACKUP_AT = "auto_backup_last_at"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    // زمان آخرین پشتیبان‌گیری خودکار موفق (epoch millis)، برای نمایش «آخرین بکاپ خودکار:
    // چند وقت پیش» در تنظیمات — این‌طور کاربر مطمئن می‌شود این قابلیت واقعاً در پس‌زمینه
    // کار می‌کند، نه فقط یک سوییچ بی‌اثر
    fun setLastBackupAt(context: Context, epochMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP_AT, epochMillis).apply()
    }

    fun getLastBackupAt(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getLong(KEY_LAST_BACKUP_AT, -1L)
        return if (value == -1L) null else value
    }
}
