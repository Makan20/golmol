package com.ordibehesht.finance.ui.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * وضعیت نمایش راهنمای اولیه (Onboarding) را نگه می‌دارد — این‌که آیا کاربر آن را
 * حداقل یک‌بار دیده یا نه. با اولین اجرای اپ روی هر دستگاه، این مقدار false است
 * و اپ به‌جای صفحه‌ی خانه، ابتدا OnboardingScreen را نشان می‌دهد (ر.ک. MainActivity).
 * بعد از پایان یا رد کردن راهنما، این مقدار true ثبت می‌شود تا در اجراهای بعدی
 * دیگر نشان داده نشود — مگر این‌که کاربر از تنظیمات دوباره‌ی آن را درخواست کند.
 */
object OnboardingPreferences {
    private const val PREFS_NAME = "narges_onboarding_prefs"
    private const val KEY_ONBOARDING_SEEN = "onboarding_seen"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeenOnboarding(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_SEEN, false)

    fun setOnboardingSeen(context: Context, seen: Boolean = true) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_SEEN, seen).apply()
    }
}
