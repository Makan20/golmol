package com.ordibehesht.finance.ui.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {

    enum class BiometricStatus {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NOT_ENROLLED,
        UNKNOWN
    }

    fun checkStatus(activity: FragmentActivity): BiometricStatus {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNKNOWN
        }
    }

    fun isBiometricAvailable(activity: FragmentActivity): Boolean =
        checkStatus(activity) == BiometricStatus.AVAILABLE

    // برخلاف بیومتریک، این چک می‌کند که آیا خودِ گوشی اصلاً قفل امنیتی (PIN/الگو/رمز) دارد یا نه.
    // این پیش‌شرط لازم برای createConfirmDeviceCredentialIntent است: روی گوشی‌های بدون قفل صفحه،
    // آن intent اصلاً چیزی برای تأیید ندارد
    fun isDeviceSecure(activity: FragmentActivity): Boolean {
        val keyguardManager =
            activity.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    // Intent استاندارد اندروید برای تأیید هویت با رمز/الگو/PIN خودِ قفل‌صفحه‌ی گوشی —
    // برای مسیرهایی که بیومتریک اپ در دسترس نیست ولی هنوز باید یک عامل احراز هویت واقعی
    // (نه صرفاً یک دکمه‌ی تأیید بی‌قیدوشرط) قبل از عملیات حساس اجرا شود
    fun createConfirmDeviceCredentialIntent(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): android.content.Intent? {
        val keyguardManager =
            activity.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        return keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "ورود با اثر انگشت",
        subtitle: String = "برای مشاهده اطلاعات مالی هویت خود را احراز کنید",
        negativeButtonText: String = "استفاده از رمز عبور",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}