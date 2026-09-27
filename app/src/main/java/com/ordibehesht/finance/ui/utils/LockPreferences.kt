package com.ordibehesht.finance.ui.utils

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object LockPreferences {
    private const val PREFS_NAME = "narges_lock_prefs"
    private const val KEY_LOCK_ENABLED = "lock_enabled"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    // باگ رفع‌شده (امنیتی): نسخه‌ی الگوریتم هش PIN را کنار خودِ هش نگه می‌داریم تا هم بشود
    // الگوریتم را در آینده باز هم تقویت کرد بدون شکستن کاربرهای موجود، و هم رکوردهای قدیمی‌تر
    // (که با الگوریتم قبلی ساخته شده‌اند) همچنان درست verify شوند. مقدار غایب یعنی نسخه‌ی ۱
    // (SHA-256 ساده — الگوریتم قدیمی).
    private const val KEY_PIN_HASH_VERSION = "pin_hash_version"
    private const val KEY_FAILED_PIN_ATTEMPTS = "failed_pin_attempts"
    private const val KEY_LOCKOUT_UNTIL_MILLIS = "lockout_until_millis"

    private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 3
    private const val LOCKOUT_DURATION_MILLIS = 30_000L

    private const val HASH_VERSION_SHA256_LEGACY = 1
    private const val HASH_VERSION_PBKDF2 = 2
    // نکته‌ی سازگاری: minSdk این پروژه ۲۴ است (Android 7.0)، در حالی‌که
    // PBKDF2WithHmacSHA256 فقط از API 26 به بعد روی اندروید در دسترس است (طبق مستندات رسمی
    // javax.crypto.SecretKeyFactory) — استفاده از آن باعث NoSuchAlgorithmException/کرش روی
    // Android 7.0/7.1 می‌شد. PBKDF2WithHmacSHA1 از API 10 به بعد در دسترس است، پس با minSdk
    // فعلی سازگار است؛ ضعف شناخته‌شده‌ی SHA-1 (تصادم/collision) برای کاربرد HMAC/PBKDF2 اینجا
    // بی‌ربط است — مقاومت اصلی در برابر brute-force از تعداد بالای iteration می‌آید، نه از
    // انتخاب SHA-1 در برابر SHA-256.
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val PBKDF2_KEY_LENGTH_BITS = 256

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    // --- PIN ---
    // PIN هرگز به‌صورت متن ساده ذخیره نمی‌شود.
    //
    // باگ رفع‌شده (امنیتی): قبلاً هش با SHA-256 + salt تصادفی محاسبه می‌شد. SHA-256 یک الگوریتم
    // سریع است (برای همین منظور طراحی نشده) و PIN هم معمولاً کوتاه است (۴ تا ۶ رقم)؛ salt جلوی
    // rainbow table را می‌گیرد اما جلوی brute-force مستقیمِ آفلاین روی یک هش سرقت‌شده را نه —
    // با CPU معمولی، هر ۱۰٬۰۰۰ حالتِ یک PIN چهاررقمی در حد میلی‌ثانیه قابل امتحان است. حالا از
    // PBKDF2 (که در جاوا/اندروید به‌صورت built-in است، بدون نیاز به کتابخانه‌ی خارجی) با تعداد
    // iteration بالا استفاده می‌شود — الگوریتمی که عمداً کند طراحی شده و دقیقاً برای همین کاربرد
    // (هش رمزهای کوتاه) ساخته شده. رکوردهای قدیمی‌تر (نسخه‌ی هش ۱) همچنان با همان الگوریتم قدیمی verify
    // می‌شوند تا کاربرانی که از قبل PIN تنظیم کرده‌اند بیرون از اپ خودشان قفل نشوند؛ به محض
    // این‌که کاربر بعداً PIN را عوض کند (setPin دوباره صدا زده شود)، هش جدید با نسخه‌ی ۲
    // (PBKDF2) ذخیره می‌شود.

    fun isPinSet(context: Context): Boolean =
        prefs(context).contains(KEY_PIN_HASH)

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hash = hashPinPbkdf2(pin, saltHex)
        prefs(context).edit()
            .putString(KEY_PIN_SALT, saltHex)
            .putString(KEY_PIN_HASH, hash)
            .putInt(KEY_PIN_HASH_VERSION, HASH_VERSION_PBKDF2)
            .apply()
        clearFailedAttempts(context)
    }

    fun clearPin(context: Context) {
        prefs(context).edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH_VERSION)
            .apply()
        clearFailedAttempts(context)
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val storedHash = prefs(context).getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs(context).getString(KEY_PIN_SALT, null) ?: return false
        val version = prefs(context).getInt(KEY_PIN_HASH_VERSION, HASH_VERSION_SHA256_LEGACY)
        val computedHash = when (version) {
            HASH_VERSION_PBKDF2 -> hashPinPbkdf2(pin, salt)
            else -> hashPinSha256Legacy(pin, salt)
        }
        return computedHash == storedHash
    }

    private fun hashPinPbkdf2(pin: String, saltHex: String): String {
        val saltBytes = saltHex.toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // الگوریتم قدیمی (نسخه‌ی هش ۱) — فقط برای verify کردن رکوردهای ساخته‌شده قبل از این تغییر
    // نگه داشته شده؛ دیگر برای ساختن هش جدید استفاده نمی‌شود (به کامنت بالای همین آبجکت مراجعه
    // کنید).
    private fun hashPinSha256Legacy(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltHex.toByteArray(Charsets.UTF_8))
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // --- محدودیت تلاش‌های ناموفق ---
    // بعد از ۳ بار رمز غلط، ۳۰ ثانیه باید صبر کرد. این جلوی حدس زدن پشت‌سرهم PIN را می‌گیرد
    // بدون این‌که کاربر را برای مدت طولانی کامل قفل کند.

    fun recordFailedPinAttempt(context: Context) {
        val p = prefs(context)
        val attempts = p.getInt(KEY_FAILED_PIN_ATTEMPTS, 0) + 1
        val editor = p.edit().putInt(KEY_FAILED_PIN_ATTEMPTS, attempts)
        if (attempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            editor.putLong(KEY_LOCKOUT_UNTIL_MILLIS, System.currentTimeMillis() + LOCKOUT_DURATION_MILLIS)
        }
        editor.apply()
    }

    fun clearFailedAttempts(context: Context) {
        prefs(context).edit()
            .putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_MILLIS, 0L)
            .apply()
    }

    /** اگر الان تو دوره‌ی قفل موقتی باشیم، چند میلی‌ثانیه تا پایانش مانده را برمی‌گرداند؛ در غیر این صورت ۰ */
    fun remainingLockoutMillis(context: Context): Long {
        val until = prefs(context).getLong(KEY_LOCKOUT_UNTIL_MILLIS, 0L)
        val remaining = until - System.currentTimeMillis()
        return remaining.coerceAtLeast(0L)
    }
}
